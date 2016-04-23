package com.ledway.barcodescannfcwriter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.TwoStatePreference;
import android.widget.EditText;

import java.util.Calendar;

/**
 * Created by togb on 2016/4/10.
 */
public class AppPreferences extends PreferenceActivity  implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.setDefaultValues(AppPreferences.this, R.xml.preferences,
                false);
        initSummary(getPreferenceScreen());


        findPreference("about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + "+886222799889"));

                AppPreferences.this.startActivity(intent);
                return true;
            }
        });
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(this);
        alertDialog.setTitle("Enter password");
        alertDialog.setView(edittext);
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password  = edittext.getText().toString();
                Calendar calendar = Calendar.getInstance();
                int month = calendar.get(Calendar.MONTH) +1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                String todayPassword =String.format ("%02d%d",month, (int)Math.pow(2, day /10 + 1) + day %10 );
                setEnableSetting(todayPassword.equals(password));
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                setEnableSetting(false);
            }
        });
        alertDialog.show();
    }

    private void setEnableSetting(boolean b) {
            PreferenceGroup pGrp = (PreferenceGroup) getPreferenceScreen();
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                Preference p = pGrp.getPreference(i);
                if (p instanceof DialogPreference || p instanceof TwoStatePreference) {
                    pGrp.getPreference(i).setEnabled(b);
                    pGrp.getPreference(i).setSelectable(b);
                }
            }

    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p) {
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            if (p.getTitle().toString().toLowerCase().contains("password"))
            {
                p.setSummary("******");
            } else {
                p.setSummary(editTextPref.getText());
            }
            editTextPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return newValue.toString().length() > 0;
                }
            });

        }
        if (p instanceof MultiSelectListPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
    }
}
