package com.letmeaccess.ble;

/* package */ class Event<P, E extends Enum<E>> {
    public final P payload;
    public final E error;

    public Event(P payload) {
        this.payload = payload;
        this.error = null;
    }

    public Event(E error) {
        this.payload = null;
        this.error = error;
    }
}