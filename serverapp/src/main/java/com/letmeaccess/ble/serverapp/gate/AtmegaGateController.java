package com.letmeaccess.ble.serverapp.gate;

import android.hardware.usb.UsbAccessory;
import android.util.Log;
import com.letmeaccess.usb.Socket;
import com.letmeaccess.usb.aoa.UsbAoaManager;


public class AtmegaGateController implements GateController {

    private UsbAoaManager mUsbAoaManager;
    private Socket mSocket;
    private boolean isUsbSocketOpen;


    /* package */ AtmegaGateController(UsbAoaManager aoaManager) {
        mUsbAoaManager = aoaManager;
    }

    @Override
    public void setup() {
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
        if (mSocket != null && isUsbSocketOpen /*&& mAoaSocket.isConnected()*/) {
            mSocket.write(new byte[]{(byte)0x01});
        }
        /*mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mAoaSocket != null && isUsbSocketOpen *//*&& mAoaSocket.isConnected()*//*) {
                    mAoaSocket.write(new byte[]{(byte)0x00});
                }
                else {
                    cout("Usb Connection Failed");
                }
            }
        }, 2000);*/
    }

    @Override
    public void close() {
        mUsbAoaManager.close();
    }


    private Socket.AccessoryListener mAccessoryListener = new Socket.AccessoryListener() {
        @Override
        public void onError(Socket.AccessoryError error) {
            isUsbSocketOpen = false;
            Log.d("Pablo","AccessoryListener.onError -> " + error.name());
        }

        @Override
        public void onOpen() {
            isUsbSocketOpen = true;
            Log.d("Pablo","AccessoryListener.onOpen()");
            //vmcInput = new VmcInput(new ChunkQueue());
            //mCurPeripheral = new Cashless2(MdbManager.this);
        }

        @Override
        public void onRead(byte[] data) {
            //cout("AccessoryListener.onRead() -> " + new String(data));
            if (isUsbSocketOpen) {
                // Feed the Vmc Action queue before being parsed
                //vmcInput.chunkQueue.offer(data);
            }
        }
    };

}
