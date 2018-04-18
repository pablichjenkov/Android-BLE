package com.letmeaccess.ble.serverapp;

import pt.joaocruz04.lib.annotations.JSoapClass;
import pt.joaocruz04.lib.annotations.JSoapReqField;


@JSoapClass(namespace = "http://tempurl.org")
public class UserStatusReq {

    @JSoapReqField(order = 0, fieldName = "cia")
    public int condo;

    @JSoapReqField(order = 1, fieldName = "usu")
    public String user;

    public UserStatusReq(int condo, String user) {
        this.condo = condo;
        this.user = user;
    }
}
