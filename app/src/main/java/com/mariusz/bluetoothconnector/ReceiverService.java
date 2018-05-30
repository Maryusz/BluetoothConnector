package com.mariusz.bluetoothconnector;


import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;



public class ReceiverService implements Runnable {
    private final static int DATA_RECEIVED = 0x0001;
    private InputStream bluetoothInputStream;
    private Handler mHandler;

    public ReceiverService(InputStream bluetoothInputStream, Handler handler) {
        this.bluetoothInputStream = bluetoothInputStream;
        mHandler = handler;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        while (true) {
            try {
                bytes = bluetoothInputStream.read(buffer);
                String receivedData = new String(buffer, 0, bytes);
                Message message = new Message();
                message.what = DATA_RECEIVED;
                message.obj = receivedData;
                mHandler.sendMessage(message);


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
