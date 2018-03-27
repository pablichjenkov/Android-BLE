package com.letmeaccess;


public interface BleSocket {

    void write(String data);

    interface Listener {
        void onRead(String data);
        void onError();
    }

    enum Error {
        ConnectionFailed,
        WriteError,
        ReadError
    }

}
