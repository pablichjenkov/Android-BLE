package com.letmeaccess.ble.serverapp.gate;

import android.hardware.usb.UsbAccessory;
import com.letmeaccess.usb.Socket;
import com.letmeaccess.usb.aoa.UsbAoaManager;


public class FT311DGpioGateController implements GateController {

    private UsbAoaManager mUsbAoaManager;
    private Socket mSocket;
    private FT311Gpio mFt311Gpio;
    private Listener mListener;


    /* package */ FT311DGpioGateController(UsbAoaManager aoaManager, Listener listener) {
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
        if (mSocket != null && mSocket.isConnected() && mFt311Gpio != null) {
            mFt311Gpio.send((byte)0xFF);
        }
        /* If we need to pull down the Voltage for the gate to close use code bellow.
        mHandler.postDelayed(new Runnable() {
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
        /*if (mFt311Gpio != null) {
            mFt311Gpio.close();
        }*/
    }


    private Socket.AccessoryListener mAccessoryListener = new Socket.AccessoryListener() {
        @Override
        public void onError(Socket.AccessoryError error) {
            mListener.onGateControllerError(Error.ConnectionFail);
        }

        @Override
        public void onOpen() {
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

            mListener.onGateControllerReady();
        }

        @Override
        public void onRead(byte[] data) {
            // Feed the Vmc Action queue before being parsed
            //vmcInput.chunkQueue.offer(data);
        }
    };

}
