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

import android.app.Activity;
/* For debug()
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
*/
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class DescriptionActivity extends Activity
	implements View.OnClickListener
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.description);
		/* debug(); */
	}

	public void onClick(View view)
	{
		/* debug(); */
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		/* Auto-generated method stub */
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.xml.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		/* Handle item selection */
		switch (item.getItemId())
		{
			case R.id.settings:
				SettingsScreen();
				return(true);
			case R.id.about:
				AboutScreen();
				return(true);
			default:
				return(super.onOptionsItemSelected(item));
		}
	}

	public void debug()
	{
		/*
		String debug_status_line;
		String debug_time_line;
		String debug_process_line;
		*/
		/*
		Context context = getApplicationContext();
		SharedPreferences shared_prefs =
			PreferenceManager.getDefaultSharedPreferences(this);
		*/
		/*
		debug_status_line= shared_prefs.getString("debug_status", "NONE");
		debug_time_line= shared_prefs.getString("debug_time", "NONE");
		debug_process_line= shared_prefs.getString("debug_process", "NONE");
		*/
		/*
		TextView debug_status = (TextView) findViewById(R.id.debug_status);
		debug_status.setText(debug_status_line);
		TextView debug_time = (TextView) findViewById(R.id.debug_time);
		debug_time.setText(debug_time_line);
		TextView debug_process = (TextView) findViewById(R.id.debug_process);
		debug_process.setText(debug_process_line);
		*/
	}

	public void SettingsScreen()
	{
		Intent myIntent = new Intent();
		myIntent.setClassName("com.vandermudellc.smartbattery",
			"com.vandermudellc.smartbattery.SettingsActivity");
		startActivity(myIntent);
	}

	public void AboutScreen()
	{
		Intent myIntent = new Intent();
		myIntent.setClassName("com.vandermudellc.smartbattery",
			"com.vandermudellc.smartbattery.AboutActivity");
		startActivity(myIntent);
	}
}
