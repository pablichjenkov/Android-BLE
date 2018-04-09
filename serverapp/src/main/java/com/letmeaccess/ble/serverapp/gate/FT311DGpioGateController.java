package com.letmeaccess.ble.serverapp.gate;

import android.hardware.usb.UsbAccessory;
import android.util.Log;
import com.letmeaccess.usb.Socket;
import com.letmeaccess.usb.aoa.UsbAoaManager;


public class FT311DGpioGateController implements GateController {

    private UsbAoaManager mUsbAoaManager;
    private Socket mSocket;
    private FT311Gpio mFt311Gpio;
    private boolean isUsbSocketOpen;


    /* package */ FT311DGpioGateController(UsbAoaManager aoaManager) {
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
        if (mSocket != null && isUsbSocketOpen && mFt311Gpio != null) {
            mFt311Gpio.send((byte)0xFF);
        }
    }

    @Override
    public void close() {
        mUsbAoaManager.close();
        /*if (mFt311Gpio != null) {
            mFt311Gpio.close();
        }*/
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

            FT311Gpio.PinOut.Builder pinOutBuilder = FT311Gpio.PinOut.Builder.create();

            FT311Gpio.PinOut pinOut = pinOutBuilder.pin0(true)
                    .pin1(false)
                    .pin2(false)
                    .pin3(false)
                    .pin4(false)
                    .pin5(false)
                    .pin6(false)
                    .build();

            mFt311Gpio = new FT311Gpio(mSocket);
            mFt311Gpio.configure(pinOut);
        }

        @Override
        public void onRead(byte[] data) {
            Log.d("Pablo","AccessoryListener.onRead() -> " + new String(data));
            // Feed the Vmc Action queue before being parsed
            //vmcInput.chunkQueue.offer(data);
        }
    };

}
