package com.mariusz.bluetoothconnector;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;


public class BluetoothConnector extends Thread {

    private final static int CONNECTION_ERROR = -100;
    private BluetoothDevice bluetoothDevice;
    private Handler mHandler;
    private BluetoothService bluetoothService;

    public BluetoothConnector(BluetoothDevice bluetoothDevice, Handler handler) {

        this.bluetoothDevice = bluetoothDevice;
        mHandler = handler;
    }

    @Override
    public void run() {
        try {

            BluetoothSocket bluetoothSocket = createBluetoothSocket(bluetoothDevice);
            bluetoothSocket.connect();

            bluetoothService = new BluetoothService(bluetoothSocket, mHandler);
            bluetoothService.start();
            try {
                bluetoothService.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            Message msg = new Message();
            msg.what = CONNECTION_ERROR;
            msg.obj = e.getMessage();
            mHandler.sendMessage(msg);

        }
    }

    public BluetoothService getBluetoothService() {
        return bluetoothService;
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MainActivity.MY_UUID);
            } catch (Exception e) {
                Message msg = new Message();
                msg.what = CONNECTION_ERROR;
                msg.obj = e.getMessage();
                mHandler.sendMessage(msg);
            }
        }
        return  device.createRfcommSocketToServiceRecord(MainActivity.MY_UUID);
    }
}
