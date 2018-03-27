package com.letmeaccess;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import java.util.List;
import timber.log.Timber;


public class BleConnection {

    private enum ConnectionState {
        Idle,
        Connecting,
        Connected,
        Error
    }

    private Context mContext;
    private Listener mListener;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mBluetoothDevice;
    private ConnectionState mConnectionState = ConnectionState.Idle;


    public BleConnection(Context context, BluetoothDevice bluetoothDevice, Listener listener) {
        mContext = context;
        mBluetoothDevice = bluetoothDevice;
        mListener = listener;
    }

    public void connect() {
        // Previously connected device.  Try to reconnect.
        if (mBluetoothGatt != null) {

            if (mBluetoothGatt.connect()) {
                Timber.d("Attempting to reconnect to remote device");
                mConnectionState = ConnectionState.Connecting;

            } else {
                mConnectionState = ConnectionState.Idle;
                mListener.onEvent(new Connection(false));
                return;
            }
        }

        Timber.d("Attempting to connect to remote device");
        mConnectionState = ConnectionState.Connecting;

        // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
        mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                boolean serviceDiscoveryStarted = mBluetoothGatt.discoverServices();
                if (serviceDiscoveryStarted) {
                    Timber.d("Attempting to start service discovery success");

                } else {
                    mConnectionState = ConnectionState.Idle;
                    Timber.d("Attempting to start service discovery fail");
                    mListener.onEvent(new Connection(false));
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = ConnectionState.Idle;
                Timber.d("Connection Dropped");
                mListener.onEvent(new Connection(false));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("onServicesDiscovered() success -> status: " + status);
                displaySupportedGattServices();
                boolean settingReadNotification = setCharacteristicNotification(true);
                if (!settingReadNotification) {
                    mConnectionState = ConnectionState.Idle;
                    Timber.d("Error setting read notification");
                    mListener.onEvent(new Connection(false));
                }

            } else {
                mConnectionState = ConnectionState.Idle;
                Timber.d("onServicesDiscovered() failed -> status: " + status);
                mListener.onEvent(new Connection(false));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mListener.onEvent(new DataRead(characteristic.getStringValue(0)));
            } else {
                mListener.onEvent(new DataRead(Event.Error.Read));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            mListener.onEvent(new DataRead(characteristic.getStringValue(0)));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mListener.onEvent(new DataWrite(characteristic.getStringValue(0)));
            } else {
                mListener.onEvent(new DataWrite(Event.Error.Read));
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Timber.d("onReliableWriteCompleted() -> status: " + status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Timber.d("onDescriptorRead() -> status: " + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Timber.d("onDescriptorWrite() -> status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mConnectionState == ConnectionState.Connecting) {
                    mConnectionState = ConnectionState.Connected;
                    mListener.onEvent(new Connection(true));
                }

            } else {
                mConnectionState = ConnectionState.Idle;
                Timber.d("onServicesDiscovered() failed -> status: " + status);
                mListener.onEvent(new Connection(false));
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Timber.d("onMtuChanged() -> status: " + status);
        }

    };

    public void displaySupportedGattServices() {
        List<BluetoothGattService> services = mBluetoothGatt.getServices();
        String serviceNameList = "";
        for (BluetoothGattService service : services) {
            serviceNameList = serviceNameList.concat(service.getUuid().toString()).concat(",");
        }
    }

    public boolean setCharacteristicNotification(boolean enabled) {
        BluetoothGattService uartService = mBluetoothGatt.getService(UARTProfile.UART_SERVICE);
        if (uartService == null) {
            Timber.d("Enable Notification fail: UART service not found!");
            return false;
        }

        BluetoothGattCharacteristic TxChar = uartService.getCharacteristic(UARTProfile.TX_READ_CHAR);
        if (TxChar == null) {
            Timber.d("Enable Notification fail: Tx charateristic not found!");
            return false;
        }

        mBluetoothGatt.setCharacteristicNotification(TxChar,enabled);

        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(UARTProfile.TX_READ_CHAR_DESC);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        return mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void writeRx(String value) {

        BluetoothGattService uartService = mBluetoothGatt.getService(UARTProfile.UART_SERVICE);

        if (uartService == null) {
            Timber.d("WriteRx fail: UART service not found!");
            mListener.onEvent(new DataWrite(Event.Error.Write));
            return;
        }

        BluetoothGattCharacteristic RxChar = uartService.getCharacteristic(UARTProfile.RX_WRITE_CHAR);
        if (RxChar == null) {
            Timber.d("WriteRx fail: UART RxChar not found!");
            mListener.onEvent(new DataWrite(Event.Error.Write));
            return;
        }

        RxChar.setValue(value);
        mBluetoothGatt.writeCharacteristic(RxChar);
    }

    public void disconnect() {
        if (mBluetoothGatt != null) {
            mConnectionState = ConnectionState.Idle;
            Timber.d("Disconnecting GATT server");
            mBluetoothGatt.disconnect();
        }
    }

    public void close() {
        if (mBluetoothGatt != null) {
            mConnectionState = ConnectionState.Idle;
            Timber.d("Closing GATT server");
            mBluetoothGatt.close();
        }
    }


    public interface Listener<T extends Event<?>> {
        void onEvent(T event);
    }

    public static class Event<T> {

        public enum Error {
            Write,
            Read,
        }

        public final Error error;
        public final T payload;

        Event(T payload) {
            this.payload = payload;
            this.error = null;
        }

        Event(Error error) {
            this.payload = null;
            this.error = error;
        }
    }

    public static class Connection extends Event<Boolean> {

        /* package*/ Connection(Boolean payload) {
            super(payload);
        }

        Connection(Error error) {
            super(error);
        }
    }

    public static class DataRead extends Event<String> {

        /* package*/ DataRead(String payload) {
            super(payload);
        }

        DataRead(Error error) {
            super(error);
        }
    }

    public static class DataWrite extends Event<String> {

        /* package*/ DataWrite(String payload) {
            super(payload);
        }

        DataWrite(Error error) {
            super(error);
        }
    }

}
