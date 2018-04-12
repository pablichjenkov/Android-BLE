package com.letmeaccess.ble.serverapp.gate;

import com.letmeaccess.usb.aoa.UsbAoaManager;


public interface GateController {

    enum AOAChip {
        Atmega328,
        Atmega2560,
        Pic24Adk,
        FT311D
    }

    enum Error {
        NoAccessoryPluggedIn,
        ConnectionFail
    }

    void setup();

    void openGate();

    void close();

    interface Listener {
        void onGateControllerReady();
        void onGateControllerError(Error error);
    }


    class Builder {

        private UsbAoaManager mAoaManager;
        private AOAChip mAoaChip;
        private Listener mListener;

        public static Builder create() {
            return new Builder();
        }

        public Builder aoaManager(UsbAoaManager aoaManager) {
            mAoaManager = aoaManager;
            return this;
        }

        public Builder listener(Listener listener) {
            mListener = listener;
            return this;
        }

        public Builder aoaChip(AOAChip aoaChip) {
            mAoaChip = aoaChip;
            return this;
        }

        public GateController build() {
            GateController gateController = null;

            if (mAoaManager != null && mListener != null && mAoaChip != null) {
                switch (mAoaChip) {
                    case Pic24Adk:
                        gateController = new Pic24AdkGateController(mAoaManager, mListener);
                        break;

                    case Atmega328:
                    case Atmega2560:
                        gateController = new AtmegaGateController(mAoaManager, mListener);
                        break;

                    case FT311D:
                        gateController = new FT311DGpioGateController(mAoaManager, mListener);
                        break;
                }
            }

            return gateController;
        }

    }

}
