package com.ledway.barcodescannfcwriter;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.ledway.barcodescannfcwriter.models.Record;

import java.text.SimpleDateFormat;

/**
 * Created by togb on 2016/4/23.
 */
public class RecordListAdapter extends ArrayAdapter<Record> {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public RecordListAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView v = (TextView) convertView;

        if (v == null) {
            LayoutInflater  vi = LayoutInflater.from(getContext());
            v = (TextView) vi.inflate(R.layout.list_item_record, null);
        }
        Record  record = getItem(position);
        String text = simpleDateFormat.format(record.wk_date) + ":\t" + record.readings;
        if (record.uploaded_datetime != null){
            text = "* " + text;
        }
        v.setText(text);
        return v;
    }


}
