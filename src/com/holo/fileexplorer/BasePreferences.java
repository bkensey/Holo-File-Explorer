package com.holo.fileexplorer;

import java.io.File;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class BasePreferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	//
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		// Generically causes all list preferences show their choice as summary,
		// courtesy of
		// http://stackoverflow.com/questions/531427/how-do-i-display-the-current-value-of-an-android-preference-in-the-preference-su
		Preference pref = findPreference(key);
		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());
		}

		if (key.equals("home_folder")) {
			File file = new File(sharedPreferences.getString(key, ""));
			if (!file.isDirectory()) {
				Toast.makeText(this, "Not a valid home folder!",
						Toast.LENGTH_SHORT).show();
			}
		} else if (key.equals("rootEnabled")) {
			
		}
	}
}
