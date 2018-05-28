package com.mariusz.bluetoothconnector;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ControllerActivity extends AppCompatActivity {

    private final static int RECEIVED_VALUE = 1;
    private final static int SEND_DATA = 2;
    private final static int CONNECTION_ERROR = -100;

    private String last_received_value = "0";

    private TextView tv_connected_device_name;
    private BluetoothDevice bluetoothDevice;
    private BluetoothConnector bluetoothConnector;
    private ToggleButton btn_stby;
    private ToggleButton btn_auto;
    private TextView tv_real_time_value;
    private TextView tv_last_value;
    private TextView tv_manual_speed_value;
    private SeekBar sb_manual_speed;
    private ProgressBar pb_connection;
    private BluetoothService bluetoothService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        tv_connected_device_name = findViewById(R.id.tv_connected_device_name);
        btn_stby = findViewById(R.id.btn_stby);
        btn_auto = findViewById(R.id.btn_auto);
        tv_real_time_value = findViewById(R.id.tv_real_time_value);
        tv_last_value = findViewById(R.id.tv_last_value);
        tv_manual_speed_value = findViewById(R.id.tv_manual_speed_value);
        sb_manual_speed = findViewById(R.id.sb_manual_speed);
        pb_connection = findViewById(R.id.pb_connection);


        bluetoothDevice = getIntent().getParcelableExtra("device");
        tv_connected_device_name.setText("Tentativo di connessione a " + bluetoothDevice.getName());

        ProgressBar pb = new ProgressBar(ControllerActivity.this);
        pb.setVisibility(View.VISIBLE);
        pb.setIndeterminate(true);

        pb_connection.setVisibility(View.VISIBLE);
        AsyncTask<Void, Void, Boolean> asyncTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                while ((bluetoothService = tryToConnect()) == null) {
                    try {
                        Thread.sleep(1200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };

        asyncTask.execute();
        pb.setVisibility(View.GONE);


        btn_stby.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    btn_auto.setChecked(false);
                    tv_last_value.setVisibility(View.INVISIBLE);

                    sendData("STBY_ON");
                } else {
                    btn_auto.setChecked(true);
                    sendData("STBY_OFF");

                }

            }
        });

        btn_auto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //TODO
                if (isChecked){
                    btn_stby.setChecked(false);
                    tv_last_value.setVisibility(View.VISIBLE);
                    tv_last_value.setText(last_received_value);
                } else {
                    btn_stby.setChecked(true);
                }
            }
        });

        sb_manual_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv_manual_speed_value.setText("" + progress+ " %");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //TODO: send data.
            }
        });

    }



    private final Handler messageHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case CONNECTION_ERROR:
                    pb_connection.setVisibility(View.GONE);
                    final AlertDialog connectionError = new AlertDialog.Builder(ControllerActivity.this).create();
                    connectionError.setTitle(R.string.connection_error);
                    connectionError.setMessage((String) msg.obj);
                    connectionError.setButton(AlertDialog.BUTTON_NEUTRAL, "Riprova a connettere", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            tryToConnect();
                            connectionError.dismiss();
                        }
                    });
                    connectionError.show();
                    break;
                case RECEIVED_VALUE:
                    last_received_value = (String) msg.obj;
                    tv_real_time_value.setText((String) msg.obj);
            }
        }
    };

    private BluetoothService tryToConnect(){

        bluetoothConnector = new BluetoothConnector(bluetoothDevice, messageHandler);
        bluetoothConnector.start();

        return bluetoothConnector.getBluetoothService();
    }

    private void sendData(String data){
        bluetoothService.write(data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothService.close();

    }
}
