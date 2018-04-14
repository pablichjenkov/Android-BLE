package com.letmeaccess.ble.serverapp.gate;

import android.os.Handler;
import com.letmeaccess.usb.Socket;


public class FT311DGpioGateController implements GateController {

    private Socket mSocket;
    private FT311Gpio mFt311Gpio;
    private Listener mListener;
    private Handler mHandler;


    /* package */ FT311DGpioGateController(Listener listener) {
        mListener = listener;
        mHandler = new Handler();
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
        if (mSocket != null && mSocket.isConnected() && mFt311Gpio != null) {
            mFt311Gpio.send((byte)0xFF);
        }
        // If we need to pull down the Voltage for the gate to close use code bellow.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSocket != null && mSocket.isConnected() && mFt311Gpio != null) {
                    mFt311Gpio.send((byte)0x00);
                }
            }
        }, 2000);
    }

    @Override
    public void close() {
        /*if (mFt311Gpio != null) {
            mFt311Gpio.reset();
        }*/
        mSocket.close();
    }


    private Socket.AccessoryListener mAccessoryListener = new Socket.AccessoryListener() {
        @Override
        public void onError(Socket.AccessoryError error) {
            mListener.onGateControllerError(Error.ConnectionFail);
        }

        @Override
        public byte[] onProvideCloseCommand() {
            return new byte[]{(byte)0x14,0,0,0};
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

            mListener.onGateControllerReady(FT311DGpioGateController.this);
        }

        @Override
        public void onRead(byte[] data) {
            // Feed the Vmc Action queue before being parsed
            //vmcInput.chunkQueue.offer(data);
        }
    };

}
