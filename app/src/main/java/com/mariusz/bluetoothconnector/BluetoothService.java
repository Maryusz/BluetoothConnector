package com.mariusz.bluetoothconnector;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothService extends Thread {
    private final static int RECEIVED_VALUE = 1;
    private final static int SEND_DATA = 2;
    private final static int CONNECTION_ERROR = -100;

    private BluetoothSocket bluetoothSocket;
    private Handler mHandler;

    private InputStream inputStream;
    private OutputStream outputStream;

    public BluetoothService(BluetoothSocket bluetoothSocket, Handler handler) {
        this.bluetoothSocket = bluetoothSocket;
        mHandler = handler;

        try {
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            Message msg = new Message();
            msg.what = CONNECTION_ERROR;
            msg.obj = "Errore apertura stream:\n" + e.getMessage();
            mHandler.sendMessage(msg);
        }

    }

    @Override
    public void run() {


        byte[] buffer = new byte[1024];
        while (true) {
            try {
                inputStream.read(buffer);

                Message msg = new Message();
                msg.what = RECEIVED_VALUE;
                msg.obj = buffer.toString();
                mHandler.sendMessage(msg);



            } catch (IOException e) {
                Message msg = new Message();
                msg.what = CONNECTION_ERROR;
                msg.obj = "Errore lettura dati:\n" + e.getMessage();
                mHandler.sendMessage(msg);
            }
        }
    }

    public void write(String string) {
        try {
            if (outputStream != null) {
                outputStream.write(string.getBytes());
            }

        } catch (IOException e) {
            Message msg = new Message();
            msg.what = CONNECTION_ERROR;
            msg.obj = "Errore scrittura:\n" + e.getMessage();
            mHandler.sendMessage(msg);
        }
    }

    public void close(){
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            Message msg = new Message();
            msg.what = CONNECTION_ERROR;
            msg.obj = "Errore chiusura socket:\n" + e.getMessage();
            mHandler.sendMessage(msg);
        }
    }

}
