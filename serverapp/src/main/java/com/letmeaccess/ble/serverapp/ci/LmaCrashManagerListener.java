package com.letmeaccess.ble.serverapp.ci;

import net.hockeyapp.android.CrashManagerListener;


public class LmaCrashManagerListener extends CrashManagerListener {

  @Override
  public boolean shouldAutoUploadCrashes() {
    return true;
  }

}