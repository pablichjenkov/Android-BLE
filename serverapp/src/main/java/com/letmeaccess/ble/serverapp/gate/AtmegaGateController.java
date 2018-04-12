package com.letmeaccess.ble.serverapp.gate;

import android.hardware.usb.UsbAccessory;
import com.letmeaccess.usb.Socket;
import com.letmeaccess.usb.aoa.UsbAoaManager;


public class AtmegaGateController implements GateController {

    private UsbAoaManager mUsbAoaManager;
    private Socket mSocket;
    private Listener mListener;


    /* package */ AtmegaGateController(UsbAoaManager aoaManager, Listener listener) {
        mUsbAoaManager = aoaManager;
        mListener = listener;
    }

    @Override
    public void setup() {

        UsbAccessory[] attachedAccessories = mUsbAoaManager.getAttachedAccessories();
        if (attachedAccessories == null || attachedAccessories.length <= 0) {
            mListener.onGateControllerError(Error.NoAccessoryPluggedIn);
        }

        mUsbAoaManager.probe(new UsbAoaManager.Listener() {
            @Override
            public void onSelectAccessory(UsbAccessory[] accessoryArray) {
                mUsbAoaManager.createSocket(accessoryArray[0], mAccessoryListener);
            }

            @Override
            public void onSocketCreated(Socket socket) {
                mSocket = socket;
                mSocket.open();
            }
        });
    }

    @Override
    public void openGate() {
        if (mSocket != null && mSocket.isConnected()) {
            mSocket.write(new byte[]{(byte)0x01});
        }
    }

    @Override
    public void close() {
        mUsbAoaManager.close();
    }


    private Socket.AccessoryListener mAccessoryListener = new Socket.AccessoryListener() {
        @Override
        public void onError(Socket.AccessoryError error) {
            mListener.onGateControllerError(Error.ConnectionFail);
        }

        @Override
        public void onOpen() {
            mListener.onGateControllerReady();
        }

        @Override
        public void onRead(byte[] data) {
            // Feed the Vmc Action queue before being parsed
            //vmcInput.chunkQueue.offer(data);
        }
    };

}
