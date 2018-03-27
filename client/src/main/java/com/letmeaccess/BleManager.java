package com.letmeaccess;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;


public final class BleManager {

    private static final int SCAN_PERIOD = 10000;

    public enum State {
        Idle,
        Created,
        Setup,
        Scanning,
    }

    private Activity mActivity;
    private Handler mHandler;
    private Listener mListener;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private State mState = State.Idle;
    private List<ScanResult> mInternalScanList;


    public static BleManager instance(Activity activity, Handler handler, Listener listener) {
        return new BleManager(activity, handler, listener);
    }

    private BleManager(Activity activity, Handler handler, Listener listener) {
        mActivity = activity;
        mHandler = handler;
        mListener = listener;
        mInternalScanList = new ArrayList<>();
    }

    public void create() {
        mState = State.Created;
        mBluetoothManager = (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    public void resume() {

        if (mState == State.Idle) {
            Timber.w("BleManager is in Idle state. Call init() method first");
            return;
        }
        else if (mState == State.Setup) {
            Timber.w("BleManager is in Setup state. No need to call it again");
            return;
        }
        else if (mState == State.Scanning) { // If init() is called while scanning, stop scanning before proceed.
            Timber.w("BleManager is in Scanning state. Call stopScan() and then call this method");
            return;
        }

        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            mListener.onInit(new BleManager.InitEvent(BleManager.InitError.LocationPermissionDenied));
            return;
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onInit(new InitEvent(InitError.BleTurnedOff));
                }
            });

            return;
        }

        if (!mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onInit(new InitEvent(InitError.BleNotSupported));
                }
            });
            return;
        }

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mState = State.Setup;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onInit(new InitEvent(Boolean.TRUE));
            }
        });
    }

    /**
     * Start scanning for BLE Advertisements (& set it up to stop after a set period of time).
     */
    public void startScanning() {
        if (mState != State.Scanning) {
            mState = State.Scanning;

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            }, SCAN_PERIOD);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Fire the start scanning event
                    ScanData scanData = new ScanData(Boolean.TRUE, mInternalScanList);
                    mListener.onScan(new ScanEvent(scanData));
                }
            });

            // Kick off a new scan.
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);

        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onScan(new ScanEvent(ScanError.ScanCalledWhileScanning));
                }
            });
        }

    }

    public void stopScanning() {
        if (mState == State.Scanning) {

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mState = State.Idle;
                    mInternalScanList.clear();
                    ScanData scanData = new ScanData(Boolean.FALSE, mInternalScanList);
                    mListener.onScan(new ScanEvent(scanData));
                }
            });

            // Stop the scan, wipe the callback.
            mBluetoothLeScanner.stopScan(mScanCallback);

        }
    }

    public BleConnection createConnection(BluetoothDevice bluetoothDevice, BleConnection.Listener listener) {
        // TODO(Pablo): Verify this device connection does not exist already.
        BleConnection newConnection = new BleConnection(mActivity, bluetoothDevice, listener);
        newConnection.connect();
        return newConnection;
    }

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // TODO(Pablo): Creating a Configuration param to pass the desired UUID to connect
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(ParcelUuid.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"));
        scanFilters.add(builder.build());

        return scanFilters;
    }

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            // TODO(Pablo): See how to merge the coming batch into our current batch
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if (mState != State.Scanning) {
                return;
            }

            for (ScanResult prevScan : mInternalScanList) {
                if (prevScan.getDevice().getAddress().equalsIgnoreCase(result.getDevice().getAddress())) {
                    return;
                }
            }
            mInternalScanList.add(result);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ScanData scanData = new ScanData(Boolean.TRUE, mInternalScanList);
                    mListener.onScan(new ScanEvent(scanData));
                }
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            mState = State.Idle;
            mInternalScanList.clear();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onScan(new ScanEvent(ScanError.ScanFailed));
                }
            });
        }
    };


    public interface Listener {
        void onInit(InitEvent initEvent);
        void onScan(ScanEvent scanEvent);
    }

    public static class InitEvent extends Event<Boolean, InitError> {
        InitEvent(Boolean payload) {
            super(payload);
        }

        InitEvent(InitError error) {
            super(error);
        }
    }

    public enum InitError {
        LocationPermissionDenied,
        BleNotSupported,
        BleTurnedOff,
    }

    public static class ScanEvent extends Event<ScanData, ScanError> {
        ScanEvent(ScanData payload) {
            super(payload);
        }

        ScanEvent(ScanError error) {
            super(error);
        }
    }

    public static class ScanData {
        public boolean isScanning;
        public List<ScanResult> scanResults;

        ScanData(boolean isScanning, List<ScanResult> scanResults) {
            this.isScanning = isScanning;
            this.scanResults = scanResults;
        }
    }

    // TODO(Pablo): Create enums for the possible scan error codes
    public enum ScanError {
        ScanFailed,
        ScanCalledWhileScanning
    }

}
