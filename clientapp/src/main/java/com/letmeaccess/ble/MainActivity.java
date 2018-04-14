package com.letmeaccess.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.letmeaccess.ble.central.BleConnection;
import com.letmeaccess.ble.central.BleManager;
import java.util.List;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final int REQ_CODE_COARSE_LOC_PERM = 1021;

    private Button sendBtn;
    //private EditText inputEdt;
    private TextView consoleTxt;
    private Handler mHandler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupView();
        createBle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeBle();
    }

    @Override
    protected void onDestroy() {

        if (isBluetoothReceiverRegistered) {
            unregisterReceiver(mBluetoothReceiver);
        }
        if (getBleManager() != null) {
            getBleManager().stopScanning();
        }
        if (mBleConnection != null) {
            mBleConnection.close();
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == REQ_CODE_COARSE_LOC_PERM) {
            // Don't do anything, when the Activity resumes it will call the startBle method.
            cout("Ble Permission Granted");
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (requestCode == REQ_CODE_COARSE_LOC_PERM) {
            cout("Ble Permission Denied");
        }
    }

    //********************************************** UI ******************************************//

    private void setupView() {
        sendBtn = findViewById(R.id.sendBtn);
        //inputEdt = findViewById(R.id.inputEdt);
        consoleTxt = findViewById(R.id.consoleTxt);

        sendBtn.setOnClickListener(mOnCLickListener);
    }

    private View.OnClickListener mOnCLickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.sendBtn:
                    onSendBtnClick();
                    break;
            }
        }
    };

    private void onSendBtnClick() {
        //String data = inputEdt.getText().toString();
        mBleConnection.writeRx("open");
    }

    public void cout(final String text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                consoleTxt.append("\n");
                consoleTxt.append(text);
            }
        });
    }

    //********************************************************************************************//

    //********************************************* BLE ******************************************//

    private BleManager mBleManager;
    private BleConnection mBleConnection;
    private boolean isBluetoothReceiverRegistered;

    private BleManager getBleManager() {
        if (mBleManager == null) {
            mBleManager = BleManager.instance(this, mHandler, mBleManagerListener);
        }
        return mBleManager;
    }

    private void createBle() {
        cout("Starting Ble");
        getBleManager().create();
    }

    private void resumeBle() {
        cout("Resuming Ble");
        getBleManager().resume();
    }

    private BleManager.Listener mBleManagerListener = new BleManager.Listener() {
        @Override
        public void onInit(BleManager.InitEvent initEvent) {

            if (initEvent.error != null) {
                switch (initEvent.error) {
                    case LocationPermissionDenied:
                        cout("Ble needs permission to Location");

                        EasyPermissions.requestPermissions(MainActivity.this
                                , getString(R.string.rationale_coarse_location)
                                , REQ_CODE_COARSE_LOC_PERM
                                , Manifest.permission.ACCESS_COARSE_LOCATION);
                        break;

                    case BleTurnedOff:
                        if (!isBluetoothReceiverRegistered) {
                            cout("Ble is turned off, Enabling it in Settings");
                            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                            registerReceiver(mBluetoothReceiver, filter);
                            isBluetoothReceiverRegistered = true;
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivity(enableBtIntent);
                        }
                        else {
                            cout("You need to Enable Bluetooth in Settings to use this app");
                        }

                        break;

                    case BleNotSupported:
                        cout("Ble Not Supported");
                        break;
                }
            }
            else {
                boolean initSuccess = initEvent.payload;
                getBleManager().startScanning();
            }
        }

        @Override
        public void onScan(BleManager.ScanEvent scanEvent) {

            if (scanEvent.error != null) {
                cout("onScan()::error -> ".concat(scanEvent.error.name()));
            } else {
                if (scanEvent.payload.isScanning) {
                    if (scanEvent.payload.scanResults.size() > 0) {

                        if (mBleConnection != null) {
                            return;
                        }

                        // TODO(Pablo): Instead of picking the first device filter by letmeaccess code matching
                        // the current user condo device code, or device address. May require connect first.
                        BluetoothDevice bluetoothDevice = scanEvent.payload.scanResults.get(0).getDevice();
                        mBleConnection = getBleManager().createConnection(bluetoothDevice, mBleConnectionListener);
                        cout("Connecting to " + bluetoothDevice.getAddress());
                    }
                    else { // Start Scanning
                        cout("Scanning Ble Start...");
                    }

                }
                else { // Stops Scanning
                    cout("Scanning Ble Finished");
                }
            }
        }



    };

    private BleConnection.Listener mBleConnectionListener = new BleConnection.Listener() {
        @Override
        public void onConnectionEvent(BleConnection.Connection event) {
            boolean connected = event.payload;
            cout("Event.Connection -> ".concat(Boolean.toString(connected)));
        }

        @Override
        public void onDataEvent(BleConnection.DataEvent event) {
            if (event instanceof BleConnection.DataRead) {
                String data = event.payload;
                cout("Event.DataRead -> ".concat(data));

            } else if (event instanceof BleConnection.DataWrite) {
                BleConnection.DataError error = event.error;
                boolean writeSuccess = (error == null);
                cout("Event.DataWrite -> ".concat(Boolean.toString(writeSuccess)));
            }
        }

    };

    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    getBleManager().resume();
                    break;

                case BluetoothAdapter.STATE_OFF:
                    // TODO(Pablo): Check if our connection exist and close it.
                    break;

                default:
                    // Do nothing
            }

        }
    };

    //********************************************************************************************//

}
