package android.serialport.api;


import android.app.Application;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.serialport.api.SerialPortFinder;

public class SerialPortPreferences extends PreferenceActivity {

	private SerialPortFinder mSerialPortFinder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MyApp mApplication = (MyApp) getApplication();
		mSerialPortFinder = mApplication.mSerialPortFinder;


		// Devices
		final ListPreference devices = (ListPreference) findPreference("DEVICE");
		String[] entries = mSerialPortFinder.getAllDevices();
		String[] entryValues = mSerialPortFinder.getAllDevicesPath();
		devices.setEntries(entries);
		devices.setEntryValues(entryValues);
		devices.setSummary(devices.getValue());
		devices.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				preference.setSummary((String) newValue);
				return true;
			}
		});

		// Baud rates
		final ListPreference baudrates = (ListPreference) findPreference("BAUDRATE");
		baudrates.setSummary(baudrates.getValue());
		baudrates
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						preference.setSummary((String) newValue);
						return true;
					}
				});
		
		Preference back = findPreference("BACK");
		back.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				SerialPortPreferences.this.finish();
				return true;
			}
		});
	}

}
