package com.mariusz.bluetoothconnector;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ControllerActivity extends AppCompatActivity {

    private final static int DATA_RECEIVED = 0x0001;

    private String last_received_value = "0";
    private BluetoothSocket socket;

    private TextView tv_connected_device_name;
    private BluetoothDevice bluetoothDevice;

    private ToggleButton btn_stby;
    private ToggleButton btn_auto;
    private ToggleButton tb_thrust;
    private ImageButton btn_thrust_left;
    private ImageButton btn_thrust_right;
    private TextView tv_real_time_value;
    private TextView tv_last_value;
    private TextView tv_manual_speed_value;
    private SeekBar sb_manual_speed;
    private ProgressBar pb_connection;
    private CheckBox cb_set_on_end;

    private InputStream inputStream;
    private OutputStream outputStream;

    private Handler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        tv_connected_device_name = findViewById(R.id.tv_connected_device_name);
        btn_stby = findViewById(R.id.btn_stby);
        btn_auto = findViewById(R.id.btn_auto);
        tb_thrust = findViewById(R.id.tb_thrust);
        btn_thrust_right = findViewById(R.id.btn_thrust_right);
        btn_thrust_left = findViewById(R.id.btn_thrust_left);
        manageThrustButtons(false);

        tv_real_time_value = findViewById(R.id.tv_real_time_value);
        tv_last_value = findViewById(R.id.tv_last_value);
        tv_manual_speed_value = findViewById(R.id.tv_manual_speed_value);
        sb_manual_speed = findViewById(R.id.sb_manual_speed);
        pb_connection = findViewById(R.id.pb_connection);
        cb_set_on_end = findViewById(R.id.cb_set_on_end);


        bluetoothDevice = getIntent().getParcelableExtra("device");
        tv_connected_device_name.setText("Tentativo di connessione a " + bluetoothDevice.getName());

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case DATA_RECEIVED:
                        String messageReceived = (String) msg.obj;
                        tv_real_time_value.setText(messageReceived);
                }
            }
        };

        tryToConnect();

        btn_stby.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    btn_auto.setChecked(false);
                    tv_last_value.setVisibility(View.INVISIBLE);
                    sendData("STBY");

                } else {
                    btn_auto.setChecked(true);
                    sendData("AUTO");

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
                if (cb_set_on_end.isChecked()) {
                    sendData(String.format("MAN_SPEED: [%d]", progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!cb_set_on_end.isChecked()) {
                    sendData(String.format("MAN_SPEED: [%d]", seekBar.getProgress()));
                }
            }
        });

        tb_thrust.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sendData("THRUST_ON");
                    manageThrustButtons(true);

                } else {
                    sendData("THRUST_OFF");
                    manageThrustButtons(false);
                }
            }
        });

        btn_thrust_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("THRUST_L");
            }
        });

        btn_thrust_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("THRUST_R");
            }
        });
    }

    private void tryToConnect(){
        pb_connection.setVisibility(View.VISIBLE);
        try {
            socket = bluetoothDevice.createRfcommSocketToServiceRecord(MainActivity.MY_UUID);
            socket.connect();

            initializeStreams();
            pb_connection.setVisibility(View.GONE);
            tv_connected_device_name.setText("Connesso a " + bluetoothDevice.getName());

            ReceiverService receiverService = new ReceiverService(inputStream, mHandler);
            new Thread(receiverService).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void manageThrustButtons(boolean enabled) {
        btn_thrust_left.setClickable(enabled);
        btn_thrust_right.setClickable(enabled);
        btn_thrust_left.setEnabled(enabled);
        btn_thrust_right.setEnabled(enabled);
    }

    private void initializeStreams() {
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendData(String data) {
        try {
            outputStream.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



