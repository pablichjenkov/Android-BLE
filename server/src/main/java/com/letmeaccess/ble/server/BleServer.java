package com.letmeaccess.ble.server;

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
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;


public class BleServer {

    public enum State {
        Idle,
        Created,
        Setup,
        Advertising
    }

    private Context mContext;
    private Handler mHandler;
    private Listener mListener;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBleAdvertiser;
    private BluetoothGattServer mGattServer;
    private BleServerConnection mServerConnection;
    private State mState = State.Idle;


    private BleServer(Context context, Handler handler, Listener listener) {
        mContext = context;
        mHandler = handler;
        mListener = listener;
    }

    public static BleServer instance(Context context, Handler handler, Listener listener) {
        return new BleServer(context, handler, listener);
    }

    public void create() {
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mState = State.Created;
    }

    public void resume() {
        if (mState == State.Idle) {
            return;
        }
        if (mState == State.Setup) {
            return;
        }
        if (mState == State.Advertising) {
            return;
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onSetup(new SetupEvent(SetupError.BleTurnedOff));
                }
            });

            return;
        }

        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onSetup(new SetupEvent(SetupError.BleNotSupported));
                }
            });

            return;
        }

        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onSetup(new SetupEvent(SetupError.BleAdvertisingNotSupported));
                }
            });

            return;
        }

        mBleAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);
        setupUartService(mGattServer);
    }

    public void startAdvertising() {
        if (mState != State.Setup) {
            return;
        }

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

    private void setupUartService(BluetoothGattServer gattServer) {
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

        gattServer.addService(UART_SERVICE);
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            mState = State.Advertising;

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onAdvertiseEven(new AdvertisingEvent(true));
                }
            });

        }

        @Override
        public void onStartFailure(int errorCode) {
            mState = State.Idle;

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onAdvertiseEven(new AdvertisingEvent(AdvertiseError.Fail));
                }
            });

        }
    };

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mState = State.Setup;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onSetup(new SetupEvent(true));
                    }
                });

            } else {
                mState = State.Idle;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onSetup(new SetupEvent(SetupError.AddingUartService));
                    }
                });

            }

        }

        @Override
        public void onConnectionStateChange(final BluetoothDevice device, final int status, final int newState) {
            super.onConnectionStateChange(device, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Only handle one connection at once.
                if (mServerConnection == null) {
                    mServerConnection = new BleServerConnection(BleServer.this, device);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onConnectionCreated(mServerConnection);
                        }
                    });
                }
                else {
                    // Cancel any extra connection.
                    mGattServer.cancelConnection(device);
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                // Only attend Disconnect events if there is a current connection and matches the
                // disconnected device, otherwise ignore.
                if (mServerConnection != null
                        && device.getAddress().equalsIgnoreCase(mServerConnection.getBluetoothDevice().getAddress())) {

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mServerConnection.dispatchConnectionChange(device, status, newState);
                            mServerConnection = null;
                        }
                    });
                }

            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device
                , int requestId
                , int offset
                , BluetoothGattCharacteristic characteristic) {

            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            if (UARTProfile.TX_READ_CHAR.equals(characteristic.getUuid())) {
                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        new byte[]{0x0f});
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mServerConnection.dispatchWriteEvent();
                }
            });

        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device
                , int requestId
                , BluetoothGattCharacteristic characteristic
                , boolean preparedWrite
                , boolean responseNeeded
                , int offset
                , final byte[] value) {

            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite
                    , responseNeeded, offset, value);

            if (UARTProfile.RX_WRITE_CHAR.equals(characteristic.getUuid())) {

                //IMP: Copy the received value to storage
                if (responseNeeded) {
                    mGattServer.sendResponse(device
                            , requestId
                            , BluetoothGatt.GATT_SUCCESS
                            , 0
                            , value);
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mServerConnection.dispatchReadEvent(new String(value));
                    }
                });

            }

        }

        @Override
        public void onNotificationSent(BluetoothDevice device, final int status) {
            super.onNotificationSent(device, status);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mServerConnection.dispatchNotificationSentEvent(status);
                }
            });

        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset
                , BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mServerConnection.dispatchDescriptorReadEvent();
                }
            });

        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId
                , BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded
                , int offset, byte[] value) {

            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            //NOTE: Its important to send response. It expects response else it will disconnect
            if (responseNeeded) {
                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        value);

            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mServerConnection.dispatchDescriptorWriteEvent();
                }
            });

        }

        //end of gatt server callback
    };

    /* package */ BluetoothGattServer getGattServer() {
        return mGattServer;
    }

    public void shutdownServer() {
        if (mGattServer != null) {
            mGattServer.close();
        }
    }


    public interface Listener {
        void onSetup(SetupEvent setupEvent);
        void onAdvertiseEven(AdvertisingEvent advEvent);
        void onConnectionCreated(BleServerConnection connection);
    }

    public static class SetupEvent extends ServerEvent<Boolean, SetupError> {

        public SetupEvent(Boolean payload) {
            super(payload);
        }

        public SetupEvent(SetupError error) {
            super(error);
        }
    }

    public enum SetupError {
        BleTurnedOff,
        BleNotSupported,
        BleAdvertisingNotSupported,
        AddingUartService
    }

    public static class AdvertisingEvent extends ServerEvent<Boolean, AdvertiseError> {

        public AdvertisingEvent(Boolean payload) {
            super(payload);
        }

        public AdvertisingEvent(AdvertiseError advError) {
            super(advError);
        }
    }

    public enum AdvertiseError {
        Fail
    }

}
