package com.ledway.btprinter.adapters;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.TextView;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by togb on 2016/6/18.
 */
public class DeviceChoiceAdepter extends BaseAdapter {
  private final String mBindDeviceName;
  private final String mBindDeviceMacAddress;
  private ArrayList<HashMap<String,String>> mData = new ArrayList<>();
  public DeviceChoiceAdepter(){
    SharedPreferences sp =
        MApp.getApplication().getSharedPreferences("bt_printer", Context.MODE_PRIVATE);
    mBindDeviceName = sp.getString("device_name", "");
    mBindDeviceMacAddress = sp.getString("mac_address", "");


    BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
    for(BluetoothDevice bluetoothDevice: pairedDevices){
      HashMap<String,String>  item = new HashMap<>();
      item.put("device_name", bluetoothDevice.getName());
      item.put("mac_address", bluetoothDevice.getAddress());
      item.put("checked", mBindDeviceMacAddress.equals(bluetoothDevice.getAddress()) ?"Y":"N");
      mData.add(item);
    }
  }


  @Override public int getCount() {
    return mData.size();
  }

  @Override public HashMap<String,String> getItem(int position) {
    return mData.get(position);
  }

  @Override public long getItemId(int position) {
    return position;
  }

  @Override public View getView(int position, View convertView, ViewGroup parent) {
    if(convertView == null){
      LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
      convertView = layoutInflater.inflate(android.R.layout.select_dialog_singlechoice, parent, false);
    }
    HashMap<String,String> data = getItem(position);
    CheckedTextView checkedTextView = (CheckedTextView) convertView;
    checkedTextView.setText(data.get("device_name"));
    checkedTextView.setChecked(data.get("checked").equals("Y"));
    return convertView;
  }
}
