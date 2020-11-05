/*
 * Copyright (C) 2010 Antony Van der Mude LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vandermudellc.smartbattery;


import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * Settings for the Smart Battery.
 */
public class SettingsActivity extends PreferenceActivity
	implements Preference.OnPreferenceChangeListener
{
	static final String KEY_DISCHARGE_LEVEL = "discharge_level";
	static final String KEY_CHARGE_LEVEL = "charge_level";
	static final String KEY_RED_YELLOW_GREEN = "red_yellow_green";
	static final String KEY_COMPUTE_STATUS = "compute_status";
	static final String TIME_UNDEFINED = "??:??";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		refresh();
	}

	public boolean onPreferenceChange(Preference pref, Object newValue)
	{
		final ListPreference listPref = (ListPreference) pref;
		final int idx = listPref.findIndexOfValue((String) newValue);
		listPref.setSummary(listPref.getEntries()[idx]);
		return(true);
	}

	public void HelpScreen()
	{
	}

	public void AboutScreen()
	{
	}

	private void refresh()
	{
		final ListPreference discharge_level =
				(ListPreference) findPreference(KEY_DISCHARGE_LEVEL);
		discharge_level.setSummary(discharge_level.getEntry());
		discharge_level.setOnPreferenceChangeListener(this);
		final ListPreference charge_level =
				(ListPreference) findPreference(KEY_CHARGE_LEVEL);
		charge_level.setSummary(charge_level.getEntry());
		charge_level.setOnPreferenceChangeListener(this);
		final ListPreference red_yellow_green =
			(ListPreference) findPreference(KEY_RED_YELLOW_GREEN);
		red_yellow_green.setSummary(red_yellow_green.getEntry());
		red_yellow_green.setOnPreferenceChangeListener(this);
	}
}
