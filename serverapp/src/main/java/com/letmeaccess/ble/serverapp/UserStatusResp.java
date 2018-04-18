package com.letmeaccess.ble.serverapp;

import pt.joaocruz04.lib.annotations.JSoapClass;
import pt.joaocruz04.lib.annotations.JSoapReqField;
import pt.joaocruz04.lib.annotations.JSoapResField;


@JSoapClass(namespace = "http://tempurl.org")
public class UserStatusResp {

    @JSoapResField(name = "f_cual_usr_statusResult")
    public String userStatus;

    public UserStatusResp(String userStatus) {
        this.userStatus = userStatus;
    }
}
