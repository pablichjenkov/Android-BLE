package com.letmeaccess.ble.serverapp.gate;

import android.hardware.usb.UsbAccessory;
import android.util.Log;
import com.letmeaccess.usb.Socket;
import com.letmeaccess.usb.aoa.UsbAoaManager;


public interface GateController {

    enum Error {
        NoAccessoryPluggedIn,
        ConnectionFail
    }

    Socket.AccessoryListener getAccessoryListener();

    void setup(Socket socket);

    void openGate();

    void close();

    interface Listener {
        void onGateControllerReady(GateController gateController);
        void onGateControllerError(Error error);
    }


    class Prober {

        private static final String AOA_MANUFACTURER_FTDI = "FTDI";
        private static final String AOA_MANUFACTURER_ATMEGA = "ATMEGA";

        private UsbAoaManager mAoaManager;
        private Listener mGateListener;
        private GateController gateController;
        private Socket.AccessoryListener accessoryListener;

        public static Prober create() {
            return new Prober();
        }

        public Prober aoaManager(UsbAoaManager aoaManager) {
            mAoaManager = aoaManager;
            return this;
        }

        public Prober listener(Listener gateListener) {
            mGateListener = gateListener;
            return this;
        }

        public void probe() {

            if (mAoaManager != null && mGateListener != null) {
                UsbAccessory[] attachedAccessories = mAoaManager.getAttachedAccessories();

                if (attachedAccessories == null || attachedAccessories.length <= 0) {
                    mGateListener.onGateControllerError(Error.NoAccessoryPluggedIn);
                }

                mAoaManager.probe(new UsbAoaManager.Listener() {

                    @Override
                    public void onSelectAccessory(UsbAccessory[] accessoryArray) {

                        String manufacturer = accessoryArray[0].getManufacturer();
                        Log.d("Pablo", manufacturer);


                        if (manufacturer.equalsIgnoreCase(AOA_MANUFACTURER_FTDI)) {
                            gateController = new FT311DGpioGateController(mGateListener);
                        }
                        else {
                            gateController = new AtmegaGateController(mGateListener);
                        }

                        accessoryListener = gateController.getAccessoryListener();
                        mAoaManager.createSocket(accessoryArray[0], accessoryListener);
                    }

                    @Override
                    public void onSocketCreated(Socket socket) {
                        gateController.setup(socket);
                    }

                });

            }

        }

        public void close() {
            mAoaManager.close();
        }
    }

}
