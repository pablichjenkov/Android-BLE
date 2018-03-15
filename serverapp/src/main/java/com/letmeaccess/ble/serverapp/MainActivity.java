package com.letmeaccess.ble.serverapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button sendBtn;
    private EditText inputEdt;
    private TextView consoleTxt;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBleAdvertiser;
    private BluetoothGattServer mGattServer;
    private BluetoothDevice mConnectedDevice;
    private boolean isAdvertising;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupView();
        setupBleHardware();

    }

    protected void onResume() {
        super.onResume();
        /*
         * Make sure bluettoth is enabled
         */

        if (isAdvertising) {
            return;
        }

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

        /*
         * Check for advertising support.
         */
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            cout("No Advertising Support.");
            finish();
            return;
        }

        mBleAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);

        // If everything is okay then start
        setupServer();
        startAdvertising();
    }


    //****************************************** UI **********************************************//

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
        String dataToSend = inputEdt.getText().toString();
        notifyTxCharacteristicChange(dataToSend);
    }

    private void cout(final String newLine) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                consoleTxt.append("\n");
                consoleTxt.append(newLine);
            }
        });

    }

    //********************************************************************************************//

    //***************************************** BLE **********************************************//

    private void setupBleHardware() {
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    private void setupServer() {
        BluetoothGattService UART_SERVICE = new BluetoothGattService(UARTProfile.UART_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic TX_READ_CHAR =
                new BluetoothGattCharacteristic(UARTProfile.TX_READ_CHAR,
                        //Read-only characteristic, supports notifications
                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ);

        //Descriptor for read notifications
        BluetoothGattDescriptor TX_READ_CHAR_DESC = new BluetoothGattDescriptor(UARTProfile.TX_READ_CHAR_DESC,
                UARTProfile.DESCRIPTOR_PERMISSION);

        TX_READ_CHAR.addDescriptor(TX_READ_CHAR_DESC);


        BluetoothGattCharacteristic RX_WRITE_CHAR =
                new BluetoothGattCharacteristic(UARTProfile.RX_WRITE_CHAR,
                        //write permissions
                        BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);


        UART_SERVICE.addCharacteristic(TX_READ_CHAR);
        UART_SERVICE.addCharacteristic(RX_WRITE_CHAR);

        mGattServer.addService(UART_SERVICE);
    }

    private void startAdvertising() {
        if (mBleAdvertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(new ParcelUuid(UARTProfile.UART_SERVICE))
                .build();

        mBleAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            cout("Peripheral Advertise Started.");
            isAdvertising = true;
            //postStatusMessage("GATT Server Ready");
        }

        @Override
        public void onStartFailure(int errorCode) {
            cout("Peripheral Advertise Failed: " + errorCode);
            isAdvertising = false;
            //postStatusMessage("GATT Server Error " + errorCode);
        }
    };

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            cout("Our gatt server service was added.");
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            cout("onConnectionStateChange "
                    + UARTProfile.getStatusDescription(status) + " "
                    + UARTProfile.getStateDescription(newState));

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //postDeviceChange(device, true);
                mConnectedDevice = device;

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //postDeviceChange(device, false);
                mConnectedDevice = null;
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device
                , int requestId
                , int offset
                , BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            cout("onCharacteristicReadRequest: " + characteristic.getUuid().toString());

            if (UARTProfile.TX_READ_CHAR.equals(characteristic.getUuid())) {
                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        new byte[]{0x0f});
            }

        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device
                , int requestId
                , BluetoothGattCharacteristic characteristic
                , boolean preparedWrite
                , boolean responseNeeded
                , int offset
                , byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite
                    , responseNeeded, offset, value);

            cout("onCharacteristicWriteRequest: " + characteristic.getUuid().toString());

            if (UARTProfile.RX_WRITE_CHAR.equals(characteristic.getUuid())) {

                //IMP: Copy the received value to storage
                if (responseNeeded) {
                    mGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            value);
                    cout("Received data on " + characteristic.getUuid().toString());
                    cout("Data value: " + bytesToHex(value));

                }

                // TODO(Pablo): Invoke onDataReceived on some Listener object.
                cout(new String(value));

            }
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            cout("onNotificationSent, status: " + status);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset
                , BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            cout("onDescriptorReadRequest()");
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId
                , BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded
                , int offset, byte[] value) {
            cout("onDescriptorWriteRequest()");
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            //NOTE: Its important to send response. It expects response else it will disconnect
            if (responseNeeded) {
                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        value);

            }

        }

        //end of gatt server callback
    };

    private void shutdownServer() {
        if (mGattServer == null) return;
        mGattServer.close();
    }

    //********************************************************************************************//

    //****************************************** Util ********************************************//

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private void notifyTxCharacteristicChange(String data) {

        if (mConnectedDevice == null) {
            cout("No connected device to notify");
            return;
        }

        BluetoothGattCharacteristic txCharacteristic = mGattServer.getService(UARTProfile.UART_SERVICE)
                .getCharacteristic(UARTProfile.TX_READ_CHAR);

        txCharacteristic.setValue(data);
        boolean isNotified = mGattServer.notifyCharacteristicChanged(mConnectedDevice, txCharacteristic, false);
        cout("Notification Sent: " + isNotified);
    }

    //Helper function converts byte array to hex string
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    //Helper function converts hex string into byte array
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    //********************************************************************************************//

}
