package com.letmeaccess.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final int RC_COARSE_LOC_PERM = 1021;
    private static final int SCAN_PERIOD = 10000;

    public enum ConnectionState {
        Idle,
        Scanning,
        Connecting,
        Connected,
        Error
    }

    private Button sendBtn;
    private EditText inputEdt;
    private TextView consoleTxt;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mLastConnectedDevice;
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
        writeRxCharacteristic(data);
    }

    private void cout(final String text) {
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
            if (mConnectionState == ConnectionState.Idle || mConnectionState == ConnectionState.Scanning) {
                connect(deviceAddress);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            mConnectionState = ConnectionState.Idle;
            cout("Scan failed with error: " + errorCode);
        }
    }

    public boolean connect(String bleDeviceAddress) {
        // Previously connected device.  Try to reconnect.
        if (mLastConnectedDevice != null
                && mLastConnectedDevice.getAddress().equalsIgnoreCase(bleDeviceAddress)
                && mBluetoothGatt != null) {

            cout("Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = ConnectionState.Connecting;
                return true;
            } else {
                mConnectionState = ConnectionState.Idle;
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(bleDeviceAddress);
        if (device == null) {
            cout("Device not found.  Unable to connect.");
            return false;
        }

        cout("Trying to create a new connection.");
        mConnectionState = ConnectionState.Connecting;

        // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        mLastConnectedDevice = device;

        return true;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                cout("Connected to GATT server.");
                mConnectionState = ConnectionState.Connected;

                boolean serviceDiscoveryStarted = mBluetoothGatt.discoverServices();
                if (serviceDiscoveryStarted) {
                    cout("Attempting to start service discovery success");
                } else {
                    cout("Attempting to start service discovery fail");
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (mConnectionState == ConnectionState.Connected) {
                    mConnectionState = ConnectionState.Idle;
                }
                cout("Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                cout("onServicesDiscovered() success -> status: " + status);
                displaySupportedGattServices();
                setCharacteristicNotification(true);

            } else {
                cout("onServicesDiscovered() failed -> status: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                cout("onCharacteristicRead() -> data: " + characteristic.getStringValue(0));
            } else {
                cout("onCharacteristicRead() -> status: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            cout("onCharacteristicChanged() -> data: " + characteristic.getStringValue(0));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            cout("onCharacteristicWrite() -> status: " + status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            cout("onDescriptorRead() -> status: " + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            cout("onDescriptorWrite() -> status: " + status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            cout("onMtuChanged() -> status: " + status);
        }

    };

    public void displaySupportedGattServices() {
        List<BluetoothGattService> services = mBluetoothGatt.getServices();
        String serviceNameList = "";
        for (BluetoothGattService service : services) {
            serviceNameList = serviceNameList.concat(service.getUuid().toString()).concat(",");
        }
    }

    public void writeRxCharacteristic(String value) {

        BluetoothGattService uartService = mBluetoothGatt.getService(UARTProfile.UART_SERVICE);

        if (uartService == null) {
            cout("WriteRx fail: UART service not found!");
            return;
        }

        BluetoothGattCharacteristic RxChar = uartService.getCharacteristic(UARTProfile.RX_WRITE_CHAR);
        if (RxChar == null) {
            cout("WriteRx fail: UART RxChar not found!");
            return;
        }

        RxChar.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(RxChar);
        cout("write TXchar - status=" + status);
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readTxCharacteristic(BluetoothGattCharacteristic characteristic) {
        cout("readCharacteristic command");
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(boolean enabled) {
        BluetoothGattService uartService = mBluetoothGatt.getService(UARTProfile.UART_SERVICE);
        if (uartService == null) {
            cout("Enable Notification fail: UART service not found!");
            return;
        }

        BluetoothGattCharacteristic TxChar = uartService.getCharacteristic(UARTProfile.TX_READ_CHAR);
        if (TxChar == null) {
            cout("Enable Notification fail: Tx charateristic not found!");
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(TxChar,true);

        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(UARTProfile.TX_READ_CHAR_DESC);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void disconnect() {
        if (mBluetoothAdapter != null && mBluetoothGatt != null) {
            cout("Disconnecting from GATT server");
            mBluetoothGatt.disconnect();
        }
    }

    public void close() {
        if (mBluetoothGatt != null) {
            mConnectionState = ConnectionState.Idle;
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    //********************************************************************************************//

}
