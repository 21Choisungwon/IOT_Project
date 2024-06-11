package com.example.iotbluetooth;

import java.io.*;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.Manifest;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1; // 블루투스 권한 요청 코드
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 2; // 위치 권한 요청 코드
    private BroadcastReceiver broadcastReceiver; // 블루투스 기기 검색 결과를 수신하는 브로드캐스트 리시버
    private BluetoothManager bluetoothManager; // 블루투스 매니저
    private BluetoothAdapter bluetoothAdapter; // 블루투스 어댑터
    public static TextView pairedList, scanList; // 페어링된 기기 목록을 표시하는 텍스트뷰
    private Button connectBtn, btnFind, btnOnSwitch; // 연결, 찾기, 스위치 버튼
    private ConnectThread thread; // 블루투스 연결을 처리하는 스레드
    private AlertDialog alertDialog; // 디바이스 목록을 표시하는 알림창
    HashMap<String, String> deviceMap = new HashMap<>(); // 검색된 기기 목록
    HashMap<String, String> pairedDeviceMap = new HashMap<>(); // 페어링된 기기 목록
    private final ActivityResultLauncher<Intent> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    showMessage(this, "블루투스 활성화");
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    showMessage(this, "취소");
                }
            });

    // 토스트 메시지를 표시하는 유틸리티 함수
    public static void showMessage(Activity activity, String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    // 주어진 기기 주소로 블루투스 기기와 연결하는 함수
    private void connectDevice(String deviceAddress) {
        if (bluetoothAdapter != null) {
            // 기기 검색을 수행중이라면 취소
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // 권한이 없는 경우, 사용자에게 권한 요청
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
            } else if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }

            // 서버의 역할을 수행 할 Device 획득
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            // UUID 선언
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            try {
                thread = new ConnectThread(uuid, device, this);
                thread.run();
                showMessage(this, device.getName() + "과 연결되었습니다.");
            } catch (Exception e) { // 연결에 실패할 경우 호출됨
                showMessage(this, "기기의 전원이 꺼져 있습니다. 기기를 확인해주세요.");
            }
        }
    }

    // 액티비티가 생성될 때 호출되는 함수
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 블루투스 매니저와 어댑터 초기화
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        pairedList = findViewById(R.id.pairedList);
        connectBtn = findViewById(R.id.btnDisConnect);
        btnFind = findViewById(R.id.btnFind);
        btnOnSwitch = findViewById(R.id.btnOnSwitch);

        // 블루투스를 지원하지 않는 장비일 경우 메시지 표시 후 종료
        if (bluetoothManager == null || bluetoothAdapter == null) {
            showMessage(MainActivity.this, "블루투스를 지원하지 않는 장비입니다.");
            finish();
            return;
        } else {
            setActivate(); // 블루투스 활성화 요청
        }

        // 각 버튼에 대한 클릭 리스너 설정
        btnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findDevice(); // 블루투스 기기 검색
            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                thread.cancel(MainActivity.this); // 연결 끊기
                pairedList.setText("Disconnected");
            }
        });

        btnOnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                thread.connectedThread.switchOn(); // 스위치 켜기/끄기
            }
        });
    }

    // 블루투스 활성화 요청 함수
    public void setActivate() {
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activityResultLauncher.launch(intent);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
                }
            } else {
                getPairedDevices(); // 페어링된 기기 목록 가져오기
            }
        }
    }

    // 블루투스 비활성화 요청 함수
    public void setDeActivate() {
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                showMessage(this, "이미 비활성화 되어 있습니다");
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // 권한이 없는 경우, 사용자에게 권한 요청
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
                }
                bluetoothAdapter.disable();
                showMessage(this, "블루투스를 비활성화 하였습니다");
            }
        }
    }

    // 페어링된 블루투스 기기 목록을 가져오는 함수
    public void getPairedDevices() {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
                }

                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                if (!pairedDevices.isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("페어링 된 블루투스 디바이스 목록");

                    List<String> list = new ArrayList<>();

                    for (BluetoothDevice bluetoothDevice : pairedDevices) {
                        pairedDeviceMap.put(bluetoothDevice.getName(), bluetoothDevice.getAddress());
                        list.add(bluetoothDevice.getName());
                    }

                    list.add("닫기");

                    final CharSequence[] charSequences = list.toArray(new CharSequence[list.size()]);
                    list.toArray(new CharSequence[list.size()]);

                    // 해당 항목을 눌렀을 때 호출되는 이벤트 리스너
                    builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 해당 디바이스와 연결하는 함수 호출
                            if (charSequences[which].toString().equals("닫기")) dialog.dismiss();
                            else
                                connectDevice(pairedDeviceMap.get(charSequences[which].toString()));
                        }
                    });

                    builder.setCancelable(false);
                    alertDialog = builder.create();
                    alertDialog.show();

                } else {
                    showMessage(this, "페어링된 기기가 없습니다.");
                }

            } else {
                showMessage(this, "블루투스가 비활성화 되어 있습니다.");
            }
        }
    }

    // 블루투스 기기 검색 함수
    public void findDevice() {
        try {
            deviceMap.clear();

            if (bluetoothAdapter != null) {
                if (!bluetoothAdapter.isEnabled()) {
                    showMessage(this, "블루투스가 비활성화되어 있습니다");
                    return;
                }
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_PERMISSION);
                    return;
                }

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION_PERMISSION);
                    return;
                }

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_FINE_LOCATION_PERMISSION);
                    return;
                }

                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                    showMessage(this, "기기검색이 중단되었습니다.");
                } else {
                    bluetoothAdapter.startDiscovery();
                    showMessage(this, "기기 검색을 시작하였습니다");

                    broadcastReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            String action = intent.getAction();

                            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                                // BluetoothDevice 객체 획득
                                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                // 기기 이름
                                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                    showMessage(MainActivity.this, "블루투스를 권한이 없습니다.");
                                    return;
                                }

                                String deviceName = device.getName();
                                String deviceHardwareAddress = device.getAddress();

                                if (deviceName != null && deviceHardwareAddress != null && !deviceMap.containsKey(deviceName)) {
                                    deviceMap.put(deviceName, deviceHardwareAddress);
                                    updateScanList();
                                }
                            }
                        }
                    };
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
                    intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                    registerReceiver(broadcastReceiver, intentFilter);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showMessage(this, "오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 검색된 블루투스 기기 목록을 업데이트하는 함수
    private void updateScanList() {
        if(alertDialog != null){
            alertDialog.dismiss();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("검색된 블루투스 디바이스 목록");
            List<String> scanlist = new ArrayList<>();

            for (String deviceName : deviceMap.keySet()) {
                scanlist.add(deviceName);
            }

            scanlist.add("닫기");

            final CharSequence[] charSequences = scanlist.toArray(new CharSequence[scanlist.size()]);
            scanlist.toArray(new CharSequence[scanlist.size()]);

            builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 해당 디바이스와 연결하는 함수 호출
                    if (charSequences[which].toString().equals("닫기")) {
                        stopDeviceDiscovery();
                    } else {
                        connectDevice(deviceMap.get(charSequences[which].toString()));
                    }
                }
            });
            builder.setCancelable(false);
            alertDialog = builder.create();
            alertDialog.show();
        }
    }

    // 블루투스 기기 검색을 중단하는 함수
    private void stopDeviceDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_PERMISSION);
            return;
        }
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            showMessage(this, "기기 검색이 중단되었습니다.");
        }
    }
}
