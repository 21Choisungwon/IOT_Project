package com.example.iotbluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class ConnectedThread extends Thread {
    private static final String TAG = "CONNECTED_THREAD";
    public final BluetoothSocket bluetoothSocket;
    public InputStream inputStream;
    public OutputStream outputStream;
    public String result = "";

    public ConnectedThread(BluetoothSocket bluetoothSocket) {
        this.bluetoothSocket = bluetoothSocket;
        try {
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            setLog(TAG, e.getMessage());
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (true) {
            try {
                bytes = inputStream.read(buffer);
                result = new String(buffer, 0, bytes);

                StringBuilder number = new StringBuilder();
                for (char c : result.toCharArray()) {
                    if (Character.isDigit(c)) {
                        number.append(c);
                    }else if(c == '.') break;
                }
                String extractedNumber = number.toString();

                double CoLevels = Double.parseDouble(extractedNumber);

                Log.d("Received_Data", extractedNumber);
                if(CoLevels<35){
                    MainActivity.pairedList.setText(result + "\n보통");
                }else if(CoLevels>=35&&CoLevels<200){
                    MainActivity.pairedList.setText(result + "\n경고");
                }else if(CoLevels>=200 && CoLevels<500){
                    MainActivity.pairedList.setText(result + "\n위험");
                }else{
                    MainActivity.pairedList.setText(result + "\n\n매우 위험");
                }
            } catch (IOException e) { // 기기와의 연결이 끊기면 호출
                setLog(TAG, "기기와의 연결이 끊겼습니다.");
                break;
            }
        }
    }
    public void switchOn() {
        try {
            // 데이터 전송
            outputStream.write("O".getBytes());
        } catch (IOException e) {
            setLog(TAG, e.getMessage());
        }
    }
    public void setLog(String tag, String message) {
        Log.d(tag, message);
    }
}