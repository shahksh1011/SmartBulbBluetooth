package com.example.kshitij.smartbulb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private String TAG= "Main Activity";
    private final static int REQUEST_ENABLE_BT = 2;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private BluetoothAdapter bluetoothAdapter;
    Boolean btScanning = false;
    UUID SERVICE_ID = UUID.fromString("df500a63-02dd-c22b-1a3d-9c57281452e0");
    //UUID SERVICE_ID = UUID.fromString("df675fb2-174a-3ed4-17fe-3fc0a8c19cbd");
    UUID CHARACTERISTIC_BULB = UUID.fromString("fb959362-f26e-43a9-927c-7e17d8fb2d8d");
    UUID CHARACTERISTIC_TEMP = UUID.fromString("0ced9345-b31f-457d-a6a2-b3db9b03e39a");
    UUID CHARACTERISTIC_BEEP = UUID.fromString("ec958823-f26e-43a9-927c-7e17d8f32a90");
    Button beepBtn, bulbOn, bulbOff;
    TextView connStatus, temperature;

   /* boolean beepOn = false;*/
    BluetoothLeScanner btScanner;
    BluetoothGatt bluetoothGatt;
    BluetoothGattService gattService;
    BluetoothGattCharacteristic temperatureGattChar, bulbGattChar, beepGattChar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        btScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect peripherals.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
        beepBtn = findViewById(R.id.beep_button);
        connStatus = findViewById(R.id.status_text_view);
        connStatus.setText("Scanning...");
        temperature = findViewById(R.id.temperature_text_view);
        bulbOn = findViewById(R.id.bulb_Switch_on);
        bulbOff = findViewById(R.id.bulb_Switch_off);

        startScanning();

        beepBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(beepGattChar!=null){
                    byte[] value = new byte[1];
                    value[0] = (byte) (1 & 0xFF);
                    beepGattChar.setValue(value);
                    bluetoothGatt.writeCharacteristic(beepGattChar);
                }else{
                    Toast.makeText(MainActivity.this, "Not connected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        bulbOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bulbGattChar!=null){
                    byte[] value = new byte[1];
                    value[0] = (byte) (0 & 0xFF);
                    bulbGattChar.setValue(value);
                    boolean status = bluetoothGatt.writeCharacteristic(bulbGattChar);
                    if(status){
                        bulbOff.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                        bulbOn.setBackgroundColor(getResources().getColor(R.color.colorDisable));
                    }
                }else{
                    Toast.makeText(MainActivity.this, "Not connected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        bulbOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bulbGattChar!=null){
                    byte[] value = new byte[1];
                    value[0] = (byte) (1 & 0xFF);
                    bulbGattChar.setValue(value);
                    boolean status = bluetoothGatt.writeCharacteristic(bulbGattChar);
                    if(status){
                        bulbOff.setBackgroundColor(getResources().getColor(R.color.colorDisable));
                        bulbOn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                    }
                }else{
                    Toast.makeText(MainActivity.this, "Not connected", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            bluetoothGatt = result.getDevice().connectGatt(MainActivity.this, false, btleGattCallback);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "Scan Batch");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "Scan Failed");
        }
    };

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            if( characteristic.getUuid().toString().equals(CHARACTERISTIC_TEMP.toString())){
                byte[] val = characteristic.getValue();
                final int i =  Character.getNumericValue(val[0]);
                final int j =  Character.getNumericValue(val[1]);
                final StringBuilder stringBuilder = new StringBuilder(val.length);
                stringBuilder.append(i);
                stringBuilder.append(j);
                stringBuilder.append(" F");
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        temperature.setText(stringBuilder);
                    }
                });
            }else if( characteristic.getUuid().toString().equals(CHARACTERISTIC_BULB.toString())){
                byte[] val = characteristic.getValue();
                final int i =  Character.getNumericValue(val[0]);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if(i==0){
                            bulbOff.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                            bulbOn.setBackgroundColor(getResources().getColor(R.color.colorDisable));
                        }else{
                            bulbOn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                            bulbOff.setBackgroundColor(getResources().getColor(R.color.colorDisable));
                        }
                    }
                });
            }else if( characteristic.getUuid().toString().equals(CHARACTERISTIC_BEEP.toString())){
                byte[] val = characteristic.getValue();
                final int i =  Character.getNumericValue(val[0]);

                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {

                    }
                });
            }
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // this will get called when a device connects or disconnects
            switch (newState) {
                case 1:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            connStatus.setText("Connection Lost. Scanning...");
                            beepGattChar = null;
                            bulbGattChar = null;
                            temperatureGattChar = null;
                        }
                    });
                case 2:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            connStatus.setText("Connected");
                        }
                    });
                    bluetoothGatt.discoverServices();

                    break;
                default:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            connStatus.setText("Scanning...");
                            beepGattChar = null;
                            bulbGattChar = null;
                            temperatureGattChar = null;
                        }
                    });
                    break;
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(characteristic.getUuid().toString().equals(CHARACTERISTIC_BEEP.toString())) {
                for (BluetoothGattDescriptor descriptor : beepGattChar.getDescriptors()) {
                    descriptor.setValue( BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
                gatt.setCharacteristicNotification(beepGattChar, true);
                Log.d(TAG, "beep write : " + status + " : " + String.valueOf(characteristic.getValue()[0]));
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            gattService = gatt.getService(SERVICE_ID);
            if(gattService != null){
                temperatureGattChar = gattService.getCharacteristic(CHARACTERISTIC_TEMP);
                bulbGattChar = gattService.getCharacteristic(CHARACTERISTIC_BULB);
                beepGattChar = gattService.getCharacteristic(CHARACTERISTIC_BEEP);
                boolean rs = gatt.readCharacteristic(bulbGattChar);
                if(!rs){
                    Log.d(TAG, "Can't read bulb Char");
                }
                for (BluetoothGattDescriptor descriptor : temperatureGattChar.getDescriptors()) {
                    descriptor.setValue( BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
                gatt.setCharacteristicNotification(temperatureGattChar, true);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if( characteristic.getUuid().toString().equals(CHARACTERISTIC_BULB.toString())){
                    byte[] val = characteristic.getValue();
                    final int i =  Character.getNumericValue(val[0]);
                    Log.d(TAG, "read "+ i);
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            if(i==0){
                                bulbOff.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                bulbOn.setBackgroundColor(getResources().getColor(R.color.colorDisable));
                            }else{
                                bulbOn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                bulbOff.setBackgroundColor(getResources().getColor(R.color.colorDisable));
                            }
                        }
                    });
                }else if( characteristic.getUuid().toString().equals(CHARACTERISTIC_BEEP.toString())){
                    byte[] val = characteristic.getValue();
                    final int i =  Character.getNumericValue(val[0]);
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            /*if(i==1){
                                beepBtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                            }else{
                                beepBtn.setBackgroundColor(getResources().getColor(R.color.colorDisable));
                            }*/
                        }
                    });
                }
            }
        }
    };

    public void startScanning() {
        Log.d(TAG, "start scanning");
        btScanning = true;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(SERVICE_ID)).build();
                List<ScanFilter> filters = new ArrayList<>();
                filters.add(scanFilter);
                final ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                Log.d(TAG,"starting....");
                btScanner.startScan(filters, settings, leScanCallback);
            }
        });
    }


    public void stopScanning() {
        Log.d(TAG, "stopping scanning");
        btScanning = false;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

}
