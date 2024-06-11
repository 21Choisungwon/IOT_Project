package com.example.iotbluetooth;

import static com.example.iotbluetooth.MainActivity.showMessage;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {
    private static final String TAG = "CONNECT_THREAD";
    private final UUID myUUID;
    private final BluetoothDevice device;
    private BluetoothSocket connectSocket;
    private final Context context;
    public ConnectedThread connectedThread;
    public ConnectThread(UUID myUUID, BluetoothDevice device, Context context) {
        this.myUUID = myUUID;
        this.device = device;
        this.context = context;
    }
    protected BluetoothSocket getConnectSocket() {
        if (connectSocket == null) {
            try {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    showMessage((Activity) context, "블루투스 권한이 없습니다.");
                    return null;
                }
                connectSocket = device.createRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return connectSocket;
    }
    @Override
    public void run() {
        try {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                showMessage((Activity) context, "블루투스 권한이 없습니다.");
                return;
            }
            getConnectSocket().connect();
            if (connectSocket != null) {
                connectedThread = new ConnectedThread(getConnectSocket());
                connectedThread.start();
            }
        } catch (IOException e) {
            try {
                if (connectSocket != null) {
                    connectSocket.close();
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            throw new RuntimeException("연결 실패", e);
        }
    }

    public void cancel(MainActivity activity) {
        try {
            if (connectSocket != null) {
                connectSocket.close();
                showMessage(activity ,"디바이스와 연결을 끊었습니다");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
