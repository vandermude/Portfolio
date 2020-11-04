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

/* import android.app.Activity; */
import java.util.Calendar;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.widget.RemoteViews;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.os.IBinder;
/* import android.view.Display;*/
/* import android.view.WindowManager; */
/* import android.util.Log; */

/**
 * Define a widget that shows the Battery statistics. To build
 * an update we spawn a background {@link Service} to perform the API queries.
 */
public class BatteryWidgetProvider extends AppWidgetProvider
{
	/* private static final String LOG_TAG = "BatteryWidgetProvider"; */
	public static String CLOCK_WIDGET_UPDATE =
		"com.vandermudellc.smartbattery.SMART_BATTERY_UPDATE";
	private static final String KEY_DISCHARGE_LEVEL = "discharge_level";
	private static final String DEFAULT_DISCHARGE_LEVEL = "10";
	private static final String KEY_CHARGE_LEVEL = "charge_level";
	private static final String DEFAULT_CHARGE_LEVEL = "100";
	private static final String KEY_RED_YELLOW_GREEN = "red_yellow_green";
	private static final String DEFAULT_RED_YELLOW_GREEN = "15-30";
	private static final int[] POWERLEVEL =
	{
		R.drawable.battery_0, R.drawable.battery_5, R.drawable.battery_10,
		R.drawable.battery_15, R.drawable.battery_20, R.drawable.battery_25,
		R.drawable.battery_30, R.drawable.battery_35, R.drawable.battery_40,
		R.drawable.battery_45, R.drawable.battery_50, R.drawable.battery_55,
		R.drawable.battery_60, R.drawable.battery_65, R.drawable.battery_70,
		R.drawable.battery_75, R.drawable.battery_80, R.drawable.battery_85,
		R.drawable.battery_90, R.drawable.battery_95, R.drawable.battery_100
	};
	private static boolean displayerror = false;
	private static BatteryWidgetSmartFns widgetdata;

	public static int status;			/* the current status constant */
	public static int health;			/* the current health constant */
	public static boolean present;		/* whether a battery is present */
	public static int level;			/* the current battery level, */
										/* from 0 to {@link #EXTRA_SCALE} */
	public static int scale;			/* the maximum battery level */
	public static int icon_small;		/* the resource ID of small status */
										/* bar icon for current battery state */
	public static int plugged;			/* indicates whether device is */
										/* plugged into a power source; */
										/* 0 means it is on battery, other */
										/* constants are different types of */
										/* power sources */
	public static int voltage;			/* the current battery voltage level */
	public static int temperature;		/* the current battery temperature */
	public static String technology;	/* the technology of current battery */
	public static boolean screenon;		/* whether the screen is on or off */
	/* values for "status" field in ACTION_BATTERY_CHANGED Intent */
	/* BATTERY_STATUS_UNKNOWN */
	/* BATTERY_STATUS_CHARGING */
	/* BATTERY_STATUS_DISCHARGING */
	/* BATTERY_STATUS_NOT_CHARGING */
	/* BATTERY_STATUS_FULL */
	/* values for "health" field in ACTION_BATTERY_CHANGED Intent */
	/* BATTERY_HEALTH_UNKNOWN */
	/* BATTERY_HEALTH_GOOD */
	/* BATTERY_HEALTH_OVERHEAT */
	/* BATTERY_HEALTH_DEAD */
	/* BATTERY_HEALTH_OVER_VOLTAGE */
	/* BATTERY_HEALTH_UNSPECIFIED_FAILURE */
	/* values for plugged field in ACTION_BATTERY_CHANGED intent */
	/* These must be powers of 2. */
	/* BATTERY_PLUGGED_AC */
	/* BATTERY_PLUGGED_USB */

	@Override
	public void onEnabled(Context context)
	{
		/* This is only called once, regardless of the number of widgets of */
		/* this type */
		/* Log.i(LOG_TAG, "tvdbg onEnabled()"); */
		super.onEnabled(context);
		AlarmManager alarmManager =
			(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.SECOND, 10);
//		calendar.add(Calendar.MINUTE, 1);
		alarmManager.setRepeating(AlarmManager.RTC,
			calendar.getTimeInMillis(), 60000, createClockTickIntent(context));
		/* Create the widget data for computing battery statistics */
		/* and initialize it */
		widgetdata = new BatteryWidgetSmartFns();
		widgetdata.BatteryWidgetSetContext(context);
	}

	@Override
	public void onDisabled(Context context)
	{
		super.onDisabled(context);
		/* Log.i(LOG_TAG, "tvdbg onDisabled()"); */
		AlarmManager alarmManager =
			(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(createClockTickIntent(context));
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds)
	{
		/* Log.i(LOG_TAG, "tvdbg onUpdate()"); */
		/* To prevent any Application Not Responding (ANR) timeouts, */
		/* we perform the update in a service */
		/* This is turned off, since update only occurs every 30 minutes */
		/* We use a one minute timer instead: CLOCK_WIDGET_UPDATE intent */
		// context.startService(new Intent(context, UpdateService.class));
	}

	@Override public void onReceive(Context context, Intent intent)
	{
		super.onReceive(context, intent);
		/* Log.i(LOG_TAG, "tvdbg onReceive() - Received intent " + intent); */
		if	(CLOCK_WIDGET_UPDATE.equals(intent.getAction()))
		{
			/* Log.i(LOG_TAG, "tvdbg onReceive() - Widget update"); */
			/* Get the widget manager and ids for this widget provider, */
			/* then call the shared update method. */
			context.startService(new Intent(context, UpdateService.class));
			/* TODO START This is probably not necessary */
			/* ComponentName thisAppWidget = */
				/* new ComponentName(context.getPackageName(), */
				/* getClass().getName()); */
			/* AppWidgetManager appWidgetManager = */
				/* AppWidgetManager.getInstance(context); */
			/* int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget); */
			/* for (int appWidgetID: ids) */
			/* { */
				/* updateAppWidget(context, appWidgetManager, appWidgetID); */
			/* } */
			/* TODO END This is probably not necessary */
		}
	}

	private PendingIntent createClockTickIntent(Context context)
	{
		Intent intent = new Intent(CLOCK_WIDGET_UPDATE);
		PendingIntent pendingIntent =
			PendingIntent.getBroadcast(context, 0, intent,
			PendingIntent.FLAG_UPDATE_CURRENT);
		return(pendingIntent);
	}

	/* TODO START This is probably not necessary */
	/* public static void updateAppWidget(Context context, */
		/* AppWidgetManager appWidgetManager, int appWidgetId) */
	/* { */
		/* RemoteViews updateViews = */
			/* new RemoteViews(context.getPackageName(), R.layout.widget_battery); */
		/* appWidgetManager.updateAppWidget(appWidgetId, updateViews); */
	/* } */
	/* TODO END This is probably not necessary */

	public static void UpdateBattery(int v_status, int v_health,
		boolean v_present, int v_level, int v_scale, int v_icon_small,
		int v_plugged, int v_voltage, int v_temperature, String v_technology)
	{
		/* Log.i(LOG_TAG, "tvdbg UpdateBattery()"); */
		status = v_status;
		health = v_health;
		present = v_present;
		level = v_level;
		scale = v_scale;
		icon_small = v_icon_small;
		plugged = v_plugged;
		voltage = v_voltage;
		temperature = v_temperature;
		technology = v_technology;
	}

	public static void UpdateScreenOn(boolean v_screenon)
	{
		screenon = v_screenon;
	}

	public static class UpdateService extends Service
	{

		@Override
		public void onStart(Intent intent, int startId)
		{
			//android.os.Debug.waitForDebugger();
			/* Log.i(LOG_TAG, "tvdbg Update Service onStart()"); */
			RemoteViews updateViews = buildUpdate(this);
			/* Push update for this widget to the home screen */
			ComponentName thisWidget =
				new ComponentName(this, BatteryWidgetProvider.class);
			AppWidgetManager manager = AppWidgetManager.getInstance(this);
			manager.updateAppWidget(thisWidget, updateViews);
			BatteryLevel();
			ScreenOnOff();
		}


		/* Build a widget update to show the current Battery status. */
		public RemoteViews buildUpdate(Context context)
		{
			/* Log.d(LOG_TAG, "tvdbg buildUpdate(): "); */
			RemoteViews updateViews = null;
			if	(displayerror == false)
			{
				if	(widgetdata == null)
				{
					widgetdata = new BatteryWidgetSmartFns();
					widgetdata.BatteryWidgetSetContext(context);
				}
				widgetdata.UpdateStats(status, health, present, level, scale,
					icon_small, plugged, voltage, temperature, technology,
					screenon);
				/* Build an update that holds the updated widget contents */
				/*
				Display display = ((WindowManager) context.
					getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
				*/
				updateViews = new RemoteViews(context.getPackageName(),
							R.layout.widget_battery);
				String discharge_level_text =
					PreferenceManager.getDefaultSharedPreferences(this).
					getString(KEY_DISCHARGE_LEVEL, DEFAULT_DISCHARGE_LEVEL);
				int discharge_level = Integer.parseInt(discharge_level_text);
				String charge_level_text =
					PreferenceManager.getDefaultSharedPreferences(this).
					getString(KEY_CHARGE_LEVEL, DEFAULT_CHARGE_LEVEL);
				int charge_level = Integer.parseInt(charge_level_text);
				String red_yellow_green =
					PreferenceManager.getDefaultSharedPreferences(this).
					getString(KEY_RED_YELLOW_GREEN, DEFAULT_RED_YELLOW_GREEN);
				int red_level =
					Integer.parseInt(red_yellow_green.substring(0, 2));
				int yellow_level =
					Integer.parseInt(red_yellow_green.substring(3));
				if	(widgetdata.BatteryPercentValue() <= red_level)
				{
					updateViews.setImageViewResource(R.id.icon,
						R.drawable.battery_red);
				}
				else
				if	(widgetdata.BatteryPercentValue() <= yellow_level)
				{
					updateViews.setImageViewResource(R.id.icon,
						R.drawable.battery_yellow);
				}
				else
				{
					updateViews.setImageViewResource(R.id.icon,
						R.drawable.battery_green);
				}
				updateViews.setTextViewText(R.id.battery_source,
						widgetdata.BatterySourceString());
				updateViews.setTextViewText(R.id.battery_percent,
						widgetdata.BatteryPercentString());
				updateViews.setTextViewText(R.id.battery_time,
						widgetdata.BatteryTime(discharge_level, charge_level));
				updateViews.setImageViewResource(R.id.level,
					POWERLEVEL[widgetdata.BatteryPercentIndex()]);
				/* When user clicks on widget, launch to Battery Stat page */
				Intent defineIntent =
					new Intent(Intent.ACTION_MAIN).addCategory(
					Intent.CATEGORY_LAUNCHER).setComponent(
					new ComponentName("com.vandermudellc.smartbattery",
					"com.vandermudellc.smartbattery.DescriptionActivity"));
				PendingIntent pendingIntent = PendingIntent.getActivity(context,
						0 /* no requestCode */, defineIntent, 0 /* no flags */);
				updateViews.setOnClickPendingIntent(R.id.widget, pendingIntent);
			}
			else
			{
				/* Didn't get data, so show error message */
				updateViews = new RemoteViews(context.getPackageName(),
					R.layout.widget_message);
				CharSequence errorMessage =
					context.getText(R.string.widget_error);
				updateViews.setTextViewText(R.id.message, errorMessage);
			}
			return(updateViews);
		}

		@Override
		public IBinder onBind(Intent intent)
		{
			/* We don't need to bind to this service */
			return(null);
		}

		private final BroadcastReceiver mBatteryLevelReceiver =
			new BatteryLevelIntentReceiver();

		private class BatteryLevelIntentReceiver extends BroadcastReceiver
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				int status = intent.getIntExtra("status", -1);
				int health = intent.getIntExtra("health", -1);
				boolean present = intent.getBooleanExtra("present", true);
				int level = intent.getIntExtra("level", -1);
				int scale = intent.getIntExtra("scale", -1);
				int icon_small = intent.getIntExtra("icon-small", -1);
				int plugged = intent.getIntExtra("plugged", -1);
				int voltage = intent.getIntExtra("voltage", -1);
				int temperature = intent.getIntExtra("temperature", -1);
				String technology = intent.getStringExtra("technology");
				BatteryWidgetProvider.UpdateBattery(status, health, present,
					level, scale, icon_small, plugged,
					voltage, temperature, technology);
			}
		}

		private void BatteryLevel()
		{
			IntentFilter batteryLevelFilter =
				new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			registerReceiver(mBatteryLevelReceiver, batteryLevelFilter);
		}

		private final BroadcastReceiver mScreenOnReceiver =
			new ScreenOnIntentReceiver();

		private class ScreenOnIntentReceiver extends BroadcastReceiver
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				BatteryWidgetProvider.UpdateScreenOn(true);
			}
		}

		private final BroadcastReceiver mScreenOffReceiver =
			new ScreenOffIntentReceiver();

		private class ScreenOffIntentReceiver extends BroadcastReceiver
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				BatteryWidgetProvider.UpdateScreenOn(false);
			}
		}

		private void ScreenOnOff()
		{
			IntentFilter screenOnFilter =
				new IntentFilter(Intent.ACTION_SCREEN_ON);
			registerReceiver(mScreenOnReceiver, screenOnFilter);
			IntentFilter screenOffFilter =
				new IntentFilter(Intent.ACTION_SCREEN_OFF);
			registerReceiver(mScreenOffReceiver, screenOffFilter);
		}
	}
}
