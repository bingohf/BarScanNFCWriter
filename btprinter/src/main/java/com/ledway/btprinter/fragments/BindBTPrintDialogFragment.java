package com.ledway.btprinter.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.ledway.btprinter.R;
import com.ledway.btprinter.adapters.DeviceChoiceAdepter;
import java.util.HashMap;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

/**
 * Created by togb on 2016/6/18.
 */
public class BindBTPrintDialogFragment extends DialogFragment {
  private PublishSubject<String> mSubject = PublishSubject.create();


  @Override public void onStart() {
    super.onStart();
    BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    if (!mBtAdapter.isEnabled()){
      Toast.makeText(getContext(), R.string.hint_enable_bluetooth, Toast.LENGTH_LONG).show();
    }

  }

  private ListView mListView;
  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater li = LayoutInflater.from(getContext());
    View layout= li.inflate(R.layout.dialog_fragment_bind_bt_printer, null);
    mListView = (ListView) layout.findViewById(R.id.list_view);
    initListView(mListView);
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setTitle(R.string.bind_bt_printer)
        .setView(layout)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            DeviceChoiceAdepter deviceChoiceAdepter = (DeviceChoiceAdepter) mListView.getAdapter();
            for(int i =0; i < deviceChoiceAdepter.getCount(); ++i){
              HashMap<String,String> data = deviceChoiceAdepter.getItem(i);
              if (data.get("checked").equals("Y")){
                SharedPreferences sp =
                    getContext().getSharedPreferences("bt_printer", Context.MODE_PRIVATE);
                sp.edit().putString("mac_address", data.get("mac_address"))
                    .putString("device_name", data.get("device_name"))
                    .apply();
                mSubject.onNext(data.get("mac_address"));
                break;
              }
            }
          }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            SharedPreferences sp =
                getContext().getSharedPreferences("bt_printer", Context.MODE_PRIVATE);
            mSubject.onNext(sp.getString("mac_address", ""));
          }
        });

    Dialog dialog = builder.create();
    return dialog;

  }

  private void initListView(ListView mListView) {
    final DeviceChoiceAdepter deviceChoiceAdepter = new DeviceChoiceAdepter();
    mListView.setAdapter(deviceChoiceAdepter);
    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        for (int i =0;i < deviceChoiceAdepter.getCount(); ++i){
          deviceChoiceAdepter.getItem(i).put("checked", i == position?"Y":"N");
        }
        deviceChoiceAdepter.notifyDataSetChanged();
      }
    });
  }
  public Observable<String> rxShow(final AppCompatActivity activity){
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override public void call(final Subscriber<? super String> subscriber) {
        mSubject = PublishSubject.create();
        show(activity.getSupportFragmentManager(),"dialog");
        mSubject.asObservable().subscribe(new Action1<String>() {
          @Override public void call(String s) {
            subscriber.onNext(s);
          }
        });
      }
    });
  }
}
