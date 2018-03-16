package com.letmeaccess.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final int RC_COARSE_LOC_PERM = 1021;
    private static final int SCAN_PERIOD = 10000;

    public enum ConnectionState {
        Idle,
        Scanning,
    }

    private Button sendBtn;
    private EditText inputEdt;
    private TextView consoleTxt;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private BleConnection mBleConnection;
    private ConnectionState mConnectionState = ConnectionState.Idle;

    private Handler mHandler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    //********************************************** UI ******************************************//

    private void setupView() {
        sendBtn = findViewById(R.id.sendBtn);
        inputEdt = findViewById(R.id.inputEdt);
        consoleTxt = findViewById(R.id.consoleTxt);

        sendBtn.setOnClickListener(mOnCLickListener);
    }

    private View.OnClickListener mOnCLickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.sendBtn:
                    onSendBtnClick();
                    break;
            }
        }
    };

    private void onSendBtnClick() {
        String data = inputEdt.getText().toString();
        mBleConnection.writeRx(data);
    }

    public void cout(final String text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                consoleTxt.append("\n");
                consoleTxt.append(text);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        cout("onPermissionsGranted:" + requestCode + ":" + perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        cout("onPermissionsDenied " + requestCode + ": " + perms);
    }

    @AfterPermissionGranted(RC_COARSE_LOC_PERM)
    private void checkPermissions() {
        if (EasyPermissions.hasPermissions(MainActivity.this
                , Manifest.permission.ACCESS_COARSE_LOCATION)) {

            setupBleHardware();

        } else {
            // Request one permission
            EasyPermissions.requestPermissions(this
                    , getString(R.string.rationale_coarse_location)
                    , RC_COARSE_LOC_PERM
                    , Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    //********************************************************************************************//

    //********************************************* BLE ******************************************//

    private void setupBleHardware() {
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        /*
         * Make sure bluettoth is enabled
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            cout("Bluetooth is disabled. Request enable");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            return;
        }

        /*
         * Check for Bluetooth LE Support
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            cout("No LE Support.");
            return;
        }

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mConnectionState == ConnectionState.Idle) {
            mConnectionState = ConnectionState.Scanning;
        }
        startScanning();
    }

    /**
     * Start scanning for BLE Advertisements (& set it up to stop after a set period of time).
     */
    public void startScanning() {
        if (mScanCallback == null) {
            // Will stop the scanning after a set time.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            }, SCAN_PERIOD);

            // Kick off a new scan.
            mScanCallback = new SampleScanCallback();
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);

            String toastText = "Scanning" + " " + TimeUnit.SECONDS.convert(SCAN_PERIOD, TimeUnit.MILLISECONDS) + " sec";
            cout(toastText);

        } else {
            cout("Already Scanning");
        }
    }

    /**
     * Stop scanning for BLE Advertisements.
     */
    public void stopScanning() {
        mConnectionState = ConnectionState.Idle;
        cout("Stopping Scanning");

        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;
    }

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
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

    private class SampleScanCallback extends ScanCallback {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            String batch = "";
            for (ScanResult scanResult : results) {
                batch = batch.concat(scanResult.getDevice().getAddress()).concat(",");
            }
            cout("onBatchResults() -> " + batch);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String deviceAddress = result.getDevice().getAddress();
            cout("onScanResult() -> " + deviceAddress);

            if (mBleConnection == null) {

                mBleConnection = new BleConnection(MainActivity.this
                        , result.getDevice()
                        , bleConnListener);

                mBleConnection.connect(deviceAddress);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            mConnectionState = ConnectionState.Idle;
            cout("Scan failed with error: " + errorCode);
        }
    }

    private BleConnection.Listener bleConnListener = new BleConnection.Listener() {
        @Override
        public void onEvent(BleConnection.Event event) {
            cout("Event -> " + event.getClass().getSimpleName() + ": " + event.payload.toString());
        }
    };

}
