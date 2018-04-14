package com.letmeaccess.ble.serverapp.gate;

import com.letmeaccess.usb.Socket;


public class AtmegaGateController implements GateController {

    private Socket mSocket;
    private Listener mListener;


    /* package */ AtmegaGateController(Listener listener) {
        mListener = listener;
    }

    @Override
    public Socket.AccessoryListener getAccessoryListener() {
        return mAccessoryListener;
    }

    @Override
    public void setup(Socket socket) {
        mSocket = socket;
        mSocket.open();
    }

    @Override
    public void openGate() {
        if (mSocket != null && mSocket.isConnected()) {
            mSocket.write(new byte[]{(byte)0x01});
        }
    }

    @Override
    public void close() {
        mSocket.close();
    }


    private Socket.AccessoryListener mAccessoryListener = new Socket.AccessoryListener() {
        @Override
        public void onError(Socket.AccessoryError error) {
            mListener.onGateControllerError(Error.ConnectionFail);
        }

        @Override
        public byte[] onProvideCloseCommand() {
            return new byte[]{(byte)'s',(byte)'h',(byte)'u',(byte)'t'};
        }

        @Override
        public void onOpen() {
            mListener.onGateControllerReady(AtmegaGateController.this);
        }

        @Override
        public void onRead(byte[] data) {
            // Feed the Vmc Action queue before being parsed
            //vmcInput.chunkQueue.offer(data);
        }
    };

}
