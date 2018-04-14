package com.letmeaccess.ble.serverapp;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;
import com.letmeaccess.ble.peripheral.BleServer;
import com.letmeaccess.ble.peripheral.BleServerConnection;
import com.letmeaccess.ble.serverapp.gate.GateController;
import com.letmeaccess.usb.Socket;
import com.letmeaccess.usb.aoa.UsbAoaManager;
import com.letmeaccess.usb.host.UsbHostManager;


public class MainActivity extends AppCompatActivity {

    private enum Stage {
        Idle,
        UsbSetup,
        BleSetup,
        Up
    }

    private Stage mStage;
    private GateController gateController;
    private TextView consoleTxt;
    private Handler mHandler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStage = Stage.Idle;
        setupView();
        setupGateController();
    }

    protected void onResume() {
        super.onResume();

        if (mStage == Stage.BleSetup) {
            resumeBleServer();
        }

    }

    @Override
    protected void onDestroy() {
        getBleServer().shutdownServer();
        if (gateController != null) {
            gateController.close();
        }

        super.onDestroy();
    }

    //****************************************** UI **********************************************//

    private void setupView() {
        consoleTxt = findViewById(R.id.consoleTxt);
        consoleTxt.setMovementMethod(new ScrollingMovementMethod());
    }

    private void onSendBtnClick() {
        mServerConnection.write("letmeaccess");
    }

    private void cout(final String newLine) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                consoleTxt.append("\n");
                consoleTxt.append(newLine);
            }
        });

    }

    //********************************************************************************************//

    //****************************************** BLE *********************************************//

    private BleServer mBleServer;
    private BleServerConnection mServerConnection;
    private boolean isBluetoothReceiverRegistered;

    private BleServer getBleServer() {
        if (mBleServer == null) {
            mBleServer = BleServer.instance(this
                    , mHandler
                    , mServerListener);
        }
        return mBleServer;
    }

    public void createBleServer() {
        cout("Starting Ble Server");
        getBleServer().create();
    }

    private void resumeBleServer() {
        cout("Resuming Ble");
        getBleServer().resume();
    }

    private BleServer.Listener mServerListener = new BleServer.Listener() {
        @Override
        public void onSetup(BleServer.SetupEvent setupEvent) {
            if (setupEvent.error != null) {
                if (setupEvent.error == BleServer.SetupError.BleNotSupported) {
                    cout("Ble not supported");
                }
                else if (setupEvent.error == BleServer.SetupError.BleAdvertisingNotSupported) {
                    cout("Ble advertising not supported");
                }
                else if (setupEvent.error == BleServer.SetupError.BleTurnedOff) {
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
                }
                else if (setupEvent.error == BleServer.SetupError.AddingUartService) {
                    cout("Ble Server error adding UART service");
                }
            }
            else {
                if (setupEvent.payload) {
                    mBleServer.startAdvertising();
                }
            }
        }

        @Override
        public void onAdvertiseEven(BleServer.AdvertisingEvent advEvent) {
            if (advEvent.payload) {
                cout("Ble Server is Advertising ...");
            } else {
                // TODO(Pablo): Restart another advertising session
                cout("Ble Server stop Advertising");
            }
        }

        @Override
        public void onConnectionCreated(BleServerConnection connection) {
            mServerConnection = connection;
            mServerConnection.setListener(mConnectionListener);
        }

    };

    private BleServerConnection.Listener mConnectionListener = new BleServerConnection.Listener() {

        @Override
        public void onConnectionEvent(BleServerConnection.ConnectionEvent connectionEvent) {
            if (connectionEvent.error != null) {
                cout("onConnectionEvent -> " + connectionEvent.error.name());
            }
            else {
                cout("onConnectionEvent -> " + Boolean.toString(connectionEvent.payload));
            }

        }

        @Override
        public void onDataEvent(BleServerConnection.DataEvent event) {
            if (event.error != null) {
                cout("Error in data transmition -> " + event.error.name());
            }
            else {
                cout("Data Event -> " + event.payload.type.name());
                switch (event.payload.type) {
                    case DescriptorWritten:

                        break;

                    case DescriptorRead:
                        break;

                    case DataRead:
                        String cmd = event.payload.value;
                        cout("Data Read Value: -> " + cmd);
                        if (cmd.equalsIgnoreCase("open")) {
                            openGate();
                        }
                        break;

                    case DataWritten:
                        break;

                    case NotificationSent:
                        break;
                }
            }
        }
    };

    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    getBleServer().resume();
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

    //****************************************** USB *********************************************//

    private UsbHostManager mUsbHostManager;
    private Socket mUsbHostSocket;

    private void setupGateController() {
        cout("Setting up Gate Controller");
        mStage = Stage.UsbSetup;

        GateController.Prober prober = GateController.Prober.create();
        prober.aoaManager(new UsbAoaManager(this)).listener(mGateControllerListener);
        prober.probe();

        //**********************************************
        //****************** USB HOST ******************
        //**********************************************
        /*mUsbHostManager = new UsbHostManager(this);
        mUsbHostManager.probe(new UsbHostManager.Listener() {
            @Override
            public void onSelectUsbDevice(Map<String, UsbDevice> usbDeviceMap) {

            }

            @Override
            public void onSocketCreated(Socket socket) {

            }

            @Override
            public UsbDeviceConfiguration onProvideDeviceConfiguration(UsbDevice usbDevice) {
                UsbDeviceConfiguration configuration = new UsbDeviceConfiguration();
                configuration.deviceInterface = usbDevice.getInterface(0);
                configuration.readEndPoint = configuration.deviceInterface.getEndpoint(0);
                configuration.writeEndPoint = configuration.deviceInterface.getEndpoint(1);

                return configuration;
            }
        });*/

    }

    private void openGate() {
        if (gateController != null) {
            cout("Opening Door");
            gateController.openGate();
        }
        else {
            cout("Error when Opening Door, GateController -> null");
        }
    }


    private GateController.Listener mGateControllerListener = new GateController.Listener() {
        @Override
        public void onGateControllerReady(GateController gateController) {
            cout("Gate Controller Setup Success");
            mStage = Stage.BleSetup;
            MainActivity.this.gateController = gateController;
            createBleServer();
            resumeBleServer();
        }

        @Override
        public void onGateControllerError(GateController.Error error) {
            cout("Gate Controller Setup Error. Make sure USB cable is plugged in.");
            mStage = Stage.Idle;
        }
    };

    //********************************************************************************************//

}
