package com.example.bluetoothtest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    String mac = "E3:D2:71:2D:A5:0A";
    private BluetoothAdapter mBleAdapter;
    private static final String UART_UUID_STR = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String UART_TX_UUID_STR = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String UART_RX_UUID_STR = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private BluetoothGatt mCurGatt;
    private byte[] receiveData;
    private BluetoothDevice mDevice;
    private static final int REQUEST_ENABLE_BT = 1000;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.connect).setOnClickListener(this);
        findViewById(R.id.scan).setOnClickListener(this);
        findViewById(R.id.disconnect).setOnClickListener(this);
        findViewById(R.id.stop).setOnClickListener(this);

        Log.i(TAG, "oncreat  走了 ");

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = bluetoothManager.getAdapter();
        if (!mBleAdapter.isEnabled()) {
            Log.i(TAG, "onClick - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "onResume  走了 ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop  走了 ");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy  走了 ");
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.connect:{
//                connect();
                startActivity(new Intent(this, FirstActivity.class));
                finish();
            }
            break;
            case R.id.scan:{
                scan();
            }
            break;
            case R.id.disconnect:{
                closeLeGatt();
            }
            break;
            case R.id.stop:{
                stop();
            }
            break;
        }

    }

    private void stop() {
        Log.e(TAG, "我点击停止扫描了");
        mBleAdapter.stopLeScan(mLeScanCallback);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void connect() {
        mCurGatt = connectLeGatt(mDevice);
    }

    private void scan() {
        mBleAdapter.startLeScan(mLeScanCallback);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.i(TAG, "device name:" + device.getName() + ", device address: " + device.getAddress() + " RSSI:" + rssi);

            if (device.getAddress().equals(mac)){
                Log.d(TAG, "找到目标设备");
                mBleAdapter.stopLeScan(mLeScanCallback);
                mDevice = device;
                mCurGatt = connectLeGatt(device);
            }

        }
    };



    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public BluetoothGatt connectLeGatt(BluetoothDevice device) {
        Log.d(TAG, "start connectLeGatt");

        BluetoothGatt gatt = device.connectGatt(this, false, new MyBluetoothGattCallback());
        if (gatt == null) {
            Log.e(TAG, "get gatt null");
            return null;
        }

        return gatt;
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void handleDeviceConnected(BluetoothGatt gatt) {
        Log.d(TAG, "handleDeviceConnected gatt:" + gatt);
        BluetoothGattService uartService = gatt.getService(UUID.fromString(UART_UUID_STR));

        BluetoothGattCharacteristic uart_ro_ch = uartService.getCharacteristic(UUID.fromString(UART_RX_UUID_STR));
        if (uart_ro_ch == null) {
            Log.e(TAG, "get uart ro character null");
            return;
        }

        //TODO
        if (!setCharacteristicNotification(gatt, uart_ro_ch, true)) {
            Log.e(TAG, "setCharacteristicNotification error");
            return;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private class MyBluetoothGattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChanged status: " + status + " newState: " + newState + " 地址值：" + this);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "BluetoothProfile.STATE_CONNECTED!!");
                // 延时一会等待稳定再连接
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Log.e(TAG, "onConnectionStateChange sleep error");
                    e.printStackTrace();
                }
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "BluetoothProfile.STATE_DISCONNECTED reconnect");

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServiceDiscovered status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleDeviceConnected(gatt);
            } else if (status != BluetoothGatt.GATT_SUCCESS) {

            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite descriptor:" + descriptor.getUuid().toString() + " in charatoristic:"
                    + descriptor.getCharacteristic().getUuid().toString());

            String uuidStr = descriptor.getCharacteristic().getUuid().toString();
            if ((UART_RX_UUID_STR).equals(uuidStr)) {
                Log.d(TAG, "write uart rx charactor notify status:" + status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "BluetoothGatt.GATT_SUCCESS:" + status);
                    } else {
                        Log.d(TAG, "BluetoothGatt.GATT_FAIL:" + status);
                    }
            } else {
                Log.e(TAG, "should not write to this charactoristic: " + uuidStr);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic ch, int status) {
            String uuidStr = ch.getUuid().toString();
            Log.d(TAG, " onCharacteristicWrite status: " + status + " uuid: " + uuidStr);
            if ((UART_TX_UUID_STR ).equals(uuidStr)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    sendAckSuccess = true;
//                    isReadyToSend = true;
//                    sendData();
                } else {
                    Log.d(TAG, "写入失败");
                }
            } else {
                Log.e(TAG, " onCharacteristicWrite not support charactoristic: " + uuidStr);
            }
        }

        // 读通知
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic ch) {
            receiveData = ch.getValue();
            Log.d(TAG, "onCharacteristicChanged: " + receiveData.toString());
            String uuidStr = ch.getUuid().toString();
            if ((UART_RX_UUID_STR ).equals(uuidStr)) {
//                receiveData(receiveData);
            } else {
                Log.e(TAG, " onCharacteristicChanged not support charactoristic: " + uuidStr);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic ch, int status) {
            Log.d(TAG, " onCharacteristicRead status: " + status + " value=" + ch.getStringValue(0));
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.e(TAG, "onReadRemoteRssi rssi=" + rssi + " status=" + status);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean setCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic ch, boolean enable) {
        Log.d(TAG, "setCharacteristicNotification");
        if (gatt == null) {
            Log.e(TAG, "setCharacteristicNotification mGatt is  null");
            return false;
        }

        BluetoothGattDescriptor descriptor = ch.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        if (null == descriptor) {
            Log.e(TAG, "setCharacteristicNotification get CLIENT_CHARACTERISTIC_CONFIG descriptor null");
            return false;
        }
        boolean ret = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (!ret) {
            Log.e(TAG, "setCharacteristicNotification setValue  for descripter  error");
            return false;
        }
        ret = gatt.writeDescriptor(descriptor);
        if (!ret) {
            Log.e(TAG, "setCharacteristicNotification writeDescriptor error");
            return false;
        }

        ret = gatt.setCharacteristicNotification(ch, enable);
        if (!ret) {
            Log.e(TAG, "setCharacteristicNotification setCharacteristicNotification error");
            return false;
        }

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void closeLeGatt() {
        if (mCurGatt != null) {
            Log.d(TAG, "closeLeGatt");
            mCurGatt.disconnect();
            mCurGatt.close();
            mCurGatt = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

}
