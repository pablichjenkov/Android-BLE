package com.letmeaccess.ble.serverapp.gate;

import com.letmeaccess.usb.aoa.UsbAoaManager;


public class Pic24AdkGateController implements GateController {

    private UsbAoaManager mAoaManager;
    private Listener mListener;


    /* package */ Pic24AdkGateController(UsbAoaManager aoaManager, Listener listener) {
        mAoaManager = aoaManager;
        mListener = listener;
    }

    @Override
    public void setup() {

    }

    @Override
    public void openGate() {

    }

    @Override
    public void close() {

    }
}
