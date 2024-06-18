package com.example.iotbluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.TextView;

public class ConnectedThread extends Thread {
    private static final String TAG = "CONNECTED_THREAD";
    public final BluetoothSocket bluetoothSocket; // 블루투스 소켓 객체
    public InputStream inputStream; // 입력 스트림 객체
    public OutputStream outputStream; // 출력 스트림 객체
    public String result = ""; // 수신된 데이터를 저장하는 문자열
    public static TextView textView_conncetDevice; // 페어링된 기기 목록을 표시하는 텍스트뷰

    // 생성자: 블루투스 소켓을 받아 스트림을 초기화
    public ConnectedThread(BluetoothSocket bluetoothSocket) {
        this.bluetoothSocket = bluetoothSocket;

        try {
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            setLog(TAG, e.getMessage());
        }
    }

    // 스레드 실행 함수: 데이터를 수신하고 처리
    @Override
    public void run() {
        byte[] buffer = new byte[1024]; // 데이터 버퍼
        int bytes; // 수신된 바이트 수

        while (true) {
            try {
                bytes = inputStream.read(buffer); // 데이터 읽기
                result = new String(buffer, 0, bytes); // 문자열로 변환
                StringBuilder number = new StringBuilder();
                // 문자열에서 숫자 추출
                for (char c : result.toCharArray()) {
                    if (Character.isDigit(c)) {
                        number.append(c);
                    } else if (c == '.') break;
                }

                String extractedNumber = number.toString(); // 추출된 숫자
                double CoLevels = Double.parseDouble(extractedNumber); // CO 수준

                Log.d("Received_Data", extractedNumber);

                // CO 수준에 따라 텍스트뷰 업데이트
                if (CoLevels < 35) {
                    MainActivity.pairedList.setText(result + "\n보통");
                } else if (CoLevels >= 35 && CoLevels < 200) {
                    MainActivity.pairedList.setText(result + "\n경고");
                } else if (CoLevels >= 200 && CoLevels < 500) {
                    MainActivity.pairedList.setText(result + "\n위험");
                } else {
                    MainActivity.pairedList.setText(result + "\n\n매우 위험");
                }
            } catch (IOException e) { // 기기와의 연결이 끊기면 호출
                setLog(TAG, "기기와의 연결이 끊겼습니다.");
                break;
            }
        }
    }

    // 스위치를 켜는 함수
    public void switchOn() {
        try {
            // 데이터 전송
            outputStream.write("O".getBytes());
        } catch (IOException e) {
            setLog(TAG, e.getMessage());
        }
    }

    // 로그를 기록하는 함수
    public void setLog(String tag, String message) {
        Log.d(tag, message);
    }
}
