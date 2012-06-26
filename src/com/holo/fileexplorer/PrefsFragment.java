package com.holo.fileexplorer;

import com.holo.fileexplorer.R;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class PrefsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.general_preferences);
        addPreferencesFromResource(R.xml.about);
        addPreferencesFromResource(R.xml.experiments_preferences);
    }
}