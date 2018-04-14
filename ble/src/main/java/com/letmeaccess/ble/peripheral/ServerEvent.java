package com.letmeaccess.ble.peripheral;

/* package */ class ServerEvent<P, E extends Enum<E>> {
    public final P payload;
    public final E error;

    public ServerEvent(P payload) {
        this.payload = payload;
        this.error = null;
    }

    public ServerEvent(E error) {
        this.payload = null;
        this.error = error;
    }
}