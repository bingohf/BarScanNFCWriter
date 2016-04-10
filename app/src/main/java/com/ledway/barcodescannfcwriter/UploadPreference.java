package com.ledway.barcodescannfcwriter;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * Created by togb on 2016/4/10.
 */
public class UploadPreference extends Preference {
    public UploadPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public UploadPreference(Context context, AttributeSet attrs) {
        this(context, attrs ,0);
    }

    public UploadPreference(Context context) {
        this(context, null);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
       // return super.onCreateView(parent);
    //    LayoutInflater li = (LayoutInflater)getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
       // return li.inflate( R.layout.preference_upload, parent, false);
        View view = LayoutInflater.from(getContext()).inflate(
                R.layout.preference_upload, parent, false);
        return view;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "xxxx", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
