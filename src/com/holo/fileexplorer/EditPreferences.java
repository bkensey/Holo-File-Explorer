/***
  Copyright (c) 2008-2012 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain	a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS,	WITHOUT	WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.
	
  From _The Busy Coder's Guide to Android Development_
    http://commonsware.com/Android
 */

package com.holo.fileexplorer;

import java.io.File;

import com.holo.fileexplorer.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;

public class EditPreferences extends BasePreferences {
	
	private int mThemeId = -1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mThemeId = extras.getInt("themeid");
		} else {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String themeTest = prefs.getString("theme", "light");

			if (themeTest.equalsIgnoreCase("light")) {
				mThemeId = R.style.AppTheme_Light;
			} else if (themeTest.equalsIgnoreCase("dark")) {
				mThemeId = R.style.AppTheme_Dark;
			} else if (themeTest.equalsIgnoreCase("darkactionbar")) {
				mThemeId = R.style.AppTheme_Light_DarkActionBar;
			}
		}		
		this.setTheme(mThemeId);
		
		super.onCreate(savedInstanceState);
		
//		ActionBar bar = getActionBar();
//		bar.setTitle("Settings");
//		bar.setDisplayHomeAsUpEnabled(true);
		
		addPreferencesFromResource(R.xml.general_preferences);
		addPreferencesFromResource(R.xml.about);
		addPreferencesFromResource(R.xml.experiments_preferences);
	}
}
