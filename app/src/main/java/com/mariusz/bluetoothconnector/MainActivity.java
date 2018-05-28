package com.mariusz.bluetoothconnector;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public final static UUID MY_UUID =  UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final static int CONTROLLER_ACTIVITY = 12;

    private ListView lv_found;
    private Button btn_enable_bt, btn_discover;
    private EditText et_logger;
    private ProgressBar pb_progress;

    ArrayAdapter<String> deviceList;
    private BluetoothAdapter bluetoothAdapter;
    private String selectedDeviceMacAddr = "";

    private boolean searchActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_enable_bt = findViewById(R.id.btn_enable_bt);
        btn_discover = findViewById(R.id.btn_discover);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBluetoothState();

        lv_found = findViewById(R.id.lv_found);
        pb_progress = findViewById(R.id.progressBar);
        et_logger = findViewById(R.id.et_logger);



        deviceList = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1);
        deviceList.add("Clicca il tasto 'CERCA'...");
        lv_found.setAdapter(deviceList);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(myReceiver, filter);


        btn_enable_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetoothIntent, 0);

                    et_logger.append("Bluetooth acceso\n");
                    btn_discover.setVisibility(View.VISIBLE);
                    btn_enable_bt.setText(R.string.btn_bt_off);

                } else {

                    bluetoothAdapter.cancelDiscovery();
                    bluetoothAdapter.disable();
                    et_logger.append("Bluetooth spento\n");
                    btn_enable_bt.setText(R.string.btn_enable_bt);

                    btn_discover.setVisibility(View.GONE);
                    pb_progress.setVisibility(View.GONE);

                }

            }
        });


        btn_discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!searchActive) {
                    bluetoothAdapter.startDiscovery();
                    pb_progress.setVisibility(View.VISIBLE);
                    et_logger.append("Dispositivi aggiunti\n");
                    deviceList.clear();
                    Set<BluetoothDevice> boundDevices = bluetoothAdapter.getBondedDevices();
                    for (BluetoothDevice bd: boundDevices) {

                        String tmp = "Accoppiato: " + bd.getName() + "\n" +bd.getAddress();
                        deviceList.add(tmp);
                        deviceList.notifyDataSetChanged();

                    }

                    searchActive = true;
                    btn_discover.setText(R.string.btn_discover_off);

                } else {
                    pb_progress.setVisibility(View.GONE);
                    searchActive = false;
                    bluetoothAdapter.cancelDiscovery();
                    btn_discover.setText(R.string.btn_discover);
                }
            }
        });


        lv_found.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    String device = deviceList.getItem(position);
                    String[] splitted = device.split("\n");
                    selectedDeviceMacAddr = splitted[1];
                    et_logger.append("MAC ADDR dispositivo selezionato: [" + splitted[1] + "]\n");
                    connect();
                } catch (ArrayIndexOutOfBoundsException e) {
                    Toast.makeText(MainActivity.this, "Selezione non valida", Toast.LENGTH_LONG).show();
                }

            }
        });

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        if(Build.VERSION.SDK_INT >= 10){
            try {
                et_logger.append("Creazione RFCOMM socket non sicuro...\n");
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                et_logger.append("Tentativo creazione socket RFCOMM non sicuro fallito\n");
            }
        }

        et_logger.append("Creazione RFCOMM socket sicuro...\n");
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                deviceList.add("Trovato: " + device.getName() + "\n" + device.getAddress());
                et_logger.append("Nuovo dispositivo: " + device.getName() + "\n");
                deviceList.notifyDataSetChanged();
            }

            et_logger.append("AZIONE RILEVATA: " + action.toString() + "\n");
        }
    };

    private void checkBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "Nessun adattatore Bluetooth. EXIT", Toast.LENGTH_LONG).show();
            btn_enable_bt.setEnabled(false);
        }

        if (bluetoothAdapter.isEnabled()) {
            btn_enable_bt.setText(R.string.btn_bt_off);
            btn_discover.setVisibility(View.VISIBLE);
        } else if (!bluetoothAdapter.isEnabled()) {
            btn_enable_bt.setText(R.string.btn_enable_bt);
        }
    }

    /**
     * This method is fundamental to create a correct socket connection.
     *
     * It checks if the passed device supports a serial port like communication (Identified as MY_UUID)
     *
     * @param bluetoothDevice the selected bluetooth device
     * @return true if the device supports the SP communications, false if not
     */
    private boolean checkForSerialPortServie(BluetoothDevice bluetoothDevice) {
        boolean containsSerialPortService = false;

        Method method = null; /// get all services
        try {
            method = bluetoothDevice.getClass().getMethod("getUuids");
            ParcelUuid[] parcelUuids = (ParcelUuid[]) method.invoke(bluetoothDevice); /// get all services
            et_logger.append("Retrived UUIDs: \n");
            for (ParcelUuid p : parcelUuids) {
                et_logger.append(p.toString() + "\n");
                if (p.getUuid().equals(MY_UUID)) {
                    containsSerialPortService = true;
                    break;
                }
            }

        } catch (NoSuchMethodException e) {
            Toast.makeText(MainActivity.this, "Errore UUID 1", Toast.LENGTH_LONG).show();
        } catch (IllegalAccessException e) {
            Toast.makeText(MainActivity.this, "Errore UUID 2", Toast.LENGTH_LONG).show();
        } catch (InvocationTargetException e) {
            Toast.makeText(MainActivity.this, "Errore UUID 3", Toast.LENGTH_LONG).show();
        }

        return containsSerialPortService;
    }

    private void connect(){

        bluetoothAdapter.cancelDiscovery();

        if (! selectedDeviceMacAddr.equals("") || !selectedDeviceMacAddr.contains(":")) {
            BluetoothDevice bd = null;
            try {
                bd = bluetoothAdapter.getRemoteDevice(selectedDeviceMacAddr);
            } catch (IllegalArgumentException ie) {
                Toast.makeText(MainActivity.this, "Scegli un dispositivo dalla lista", Toast.LENGTH_LONG).show();
            }

            if (bd == null) {
                Toast.makeText(MainActivity.this, "Nessun dispositivo connesso..", Toast.LENGTH_LONG).show();
            }

            if (checkForSerialPortServie(bd)) {
                try {
                    BluetoothSocket bluetoothSocket = createBluetoothSocket(bd);

                    et_logger.append("Tentativo connessione a " + bd.getAddress() + "\n");

                    //bluetoothSocket.connect();
                    //bluetoothSocket.close();

                    Toast.makeText(MainActivity.this, "Connessione operativa", Toast.LENGTH_SHORT).show();

                    Intent startControllerActivity = new Intent(MainActivity.this, ControllerActivity.class);
                    startControllerActivity.putExtra("device", bd);
                    startActivityForResult(startControllerActivity, CONTROLLER_ACTIVITY);
                    finish();


                } catch (IOException e) {
                    final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle(R.string.connection_failed);
                    alertDialog.setMessage("Il dispositivo dispone del servizio sulla porta seriale ma " +
                            "la connessione non è riuscita, il dispositivo potrebbe essere stato spento, " +
                            "essere fuori portata oppure avere altri problemi.\n" +
                            "Prova a riavviare o riaccopiare il dispositivo.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            alertDialog.dismiss();
                        }
                    });
                    alertDialog.show();

                }
            } else {
                final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle(R.string.ser_comm_not_supported);
                alertDialog.setMessage("Il dispositivo selezionato non supporta o non dispone del servizio " +
                        "necessario per la comunicazione seriale.\n" +
                        "Si prega di selezionare un dispositivo compatibile.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });
                alertDialog.show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(myReceiver);
    }
}
