package com.letmeaccess.ble.peripheral;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;


public class BleServerConnection {

    private BleServer mServer;
    private BluetoothDevice mBluetoothDevice;
    private Listener mListener;


    public BleServerConnection(BleServer server, BluetoothDevice bluetoothDevice) {
        mServer = server;
        mBluetoothDevice = bluetoothDevice;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void write(String data) {
        BluetoothGattServer gattServer = mServer.getGattServer();

        BluetoothGattCharacteristic txCharacteristic = gattServer
                .getService(UARTProfile.UART_SERVICE)
                .getCharacteristic(UARTProfile.TX_READ_CHAR);

        txCharacteristic.setValue(data);

        gattServer.notifyCharacteristicChanged(mBluetoothDevice
                , txCharacteristic
                , false);
    }


    // region: BleServer interaction

    /* package */ BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }


    /* package */ void dispatchConnectionChange(BluetoothDevice device, int status, int newState) {
        mListener.onConnectionEvent(new ConnectionEvent(ConnectionError.SignalFailed));
    }

    /* package */ void dispatchDescriptorWriteEvent() {
        mListener.onDataEvent(new DataEvent(new DataInfo(DataEventType.DescriptorWritten, null)));
    }

    /* package */ void dispatchDescriptorReadEvent() {
        mListener.onDataEvent(new DataEvent(new DataInfo(DataEventType.DescriptorRead, null)));
    }

    /* package */ void dispatchReadEvent(String data) {
        mListener.onDataEvent(new DataEvent(new DataInfo(DataEventType.DataRead, data)));
    }

    /* package */ void dispatchWriteEvent() {
        mListener.onDataEvent(new DataEvent(new DataInfo(DataEventType.DataWritten, null)));
    }

    /* package */ void dispatchNotificationSentEvent(int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            mListener.onDataEvent(new DataEvent(new DataInfo(DataEventType.NotificationSent, null)));
        }
        else {
            mListener.onDataEvent(new DataEvent(DataError.Write));
        }

    }

    // endregion


    public interface Listener {
        void onConnectionEvent(ConnectionEvent connectionEvent);
        void onDataEvent(DataEvent event);
    }

    public static class ConnectionEvent extends ServerEvent<Boolean, ConnectionError> {

        /* package*/ ConnectionEvent(Boolean payload) {
            super(payload);
        }

        ConnectionEvent(ConnectionError error) {
            super(error);
        }
    }

    public enum ConnectionError {
        SignalFailed,
    }

    public static class DataEvent extends ServerEvent<DataInfo, DataError> {

        /* package*/ DataEvent(DataInfo payload) {
            super(payload);
        }

        DataEvent(DataError error) {
            super(error);
        }
    }

    public static class DataInfo {
        public final DataEventType type; // true: write, false: read
        public final String value;

        DataInfo(DataEventType type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    public enum DataEventType {
        NotificationSent,
        DataWritten,
        DataRead,
        DescriptorWritten,
        DescriptorRead
    }

    public enum DataError {
        Write
    }

}
