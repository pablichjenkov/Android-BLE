package com.letmeaccess.ble.client;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import java.util.UUID;


public class UARTProfile {
    //Service UUID to expose our UART characteristics
    public static UUID UART_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");

    //RX, Write characteristic
    public static UUID RX_WRITE_CHAR = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    //TX READ Notify
    public static UUID TX_READ_CHAR = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static UUID TX_READ_CHAR_DESC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static String getStateDescription(int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return "Connected";
            case BluetoothProfile.STATE_CONNECTING:
                return "Connecting";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "Disconnected";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "Disconnecting";
            default:
                return "Unknown State " + state;
        }
    }


    public static String getStatusDescription(int status) {
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                return "SUCCESS";
            default:
                return "Unknown Status " + status;
        }
    }


}
