package com.example.kshitij.smartbulb;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceScanActivity extends ListActivity {
    private BluetoothAdapter bluetoothAdapter;
    private boolean mScanning;
    private Handler handler;

    private LeDeviceListAdapter leDeviceListAdapter;
    private BluetoothAdapter.LeScanCallback leScanCallback;
    static String UUID = "dfb553f2-d0a9-0241-4cbc-71aeba4b807c";
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);
        leScanCallback = new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(final BluetoothDevice device, int rssi,
                                         byte[] scanRecord) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                leDeviceListAdapter.addDevice(device);
                                leDeviceListAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                };
    }



    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }
        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }
        public void clear() {
            mLeDevices.clear();
        }
        @Override
        public int getCount() {
            return mLeDevices.size();
        }
        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("Unknown Device");
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }



}
