package com.letmeaccess.ble.serverapp.gate;

import com.letmeaccess.usb.Socket;


public class FT311Gpio {

    private Socket socket;
    private byte pinoutMap;
    private byte []	writeusbdata;

    public FT311Gpio(Socket socket) {
        this.socket = socket;
        writeusbdata = new byte[4];
    }

    public void configure(PinOut pinOut) {
        pinoutMap = pinOut.pinoutMap;
        byte inPutMap = (byte)(~pinoutMap);

        pinoutMap |= 0x80; 	// GPIO pin 7 is OUT
        inPutMap &= 0x7F;	// GPIO pin 7 is OUT

        writeusbdata[0] = 0x11;
        writeusbdata[1] = 0x00;
        writeusbdata[2] = pinoutMap;
        writeusbdata[3] = inPutMap;

        socket.write(writeusbdata);
    }

    public void send(byte portData) {
        portData &= pinoutMap;
        portData |= 0x80; // GPIO pin 7 is high then LED is OFF
        writeusbdata[0] = 0x13;
        writeusbdata[1] = portData;
        writeusbdata[2] = 0x00;
        writeusbdata[3] = 0x00;

        socket.write(writeusbdata);
    }

    public void close() {
        writeusbdata[0] = 0x14;
        writeusbdata[1] = 0x00;
        writeusbdata[2] = 0x00;
        writeusbdata[3] = 0x00;

        socket.write(writeusbdata);
    }


    public static class PinOut {

        public final byte pinoutMap;

        private PinOut(byte pinoutMap) {
            this.pinoutMap = pinoutMap;
        }


        public static class Builder {

            // By default all are input except pin 7 which is always output.
            private byte builderPinOutMap = (byte)0x80;

            public static Builder create() {
                return new Builder();
            }

            public Builder pin(int bit, boolean outPut) {
                if (bit >= 0 && bit < 7) {
                    if (outPut) {
                        builderPinOutMap |= (1<<bit);
                    }
                    else {
                        builderPinOutMap &= ~(1<<bit);
                    }
                }

                return this;
            }

            public Builder pin0(boolean outPut) {
                return pin(0, outPut);
            }

            public Builder pin1(boolean outPut) {
                return pin(1, outPut);
            }

            public Builder pin2(boolean outPut) {
                return pin(2, outPut);
            }

            public Builder pin3(boolean outPut) {
                return pin(3, outPut);
            }

            public Builder pin4(boolean outPut) {
                return pin(4, outPut);
            }

            public Builder pin5(boolean outPut) {
                return pin(5, outPut);
            }

            public Builder pin6(boolean outPut) {
                return pin(6, outPut);
            }

            public PinOut build() {
                return new PinOut(builderPinOutMap);
            }
        }

    }

}
