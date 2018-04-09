package com.letmeaccess.ble.serverapp.gate;

import com.letmeaccess.usb.aoa.UsbAoaManager;


public class Pic24AdkGateController implements GateController {

    private UsbAoaManager aoaManager;


    /* package */ Pic24AdkGateController(UsbAoaManager aoaManager) {
        this.aoaManager = aoaManager;
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
