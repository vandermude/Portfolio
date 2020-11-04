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
/*============================================================================*/
/* NOTE: remeber for reading and writing data, Java chars are two bytes */
/* NOTE: for computing times, charge times are 0 for 100% and discharge */
/*	times are 0 for 0%, because these are the end goals */
/* NOTE: all statistics come from input data, or are computed from saved data */
/* IMMEDIATE STATISTICS (listed in page, or as option on widget): */
/* status: UNKNOWN, CHARGING, DISCHARGING, NOT_CHARGING, FULL */
/* health: UNKNOWN, GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, UNSPECIFIED_FAILURE */
/* power */
/* voltage */
/* temperature */
/* technology */
/* raw power level */
/* COMPUTED STATISTICS (computed every minute for widget or when page opened: */
/* interpolated power level */
/* typical power level when user recharges */
/* discharge time (till 0% or typical recharge) for full typical sleep off */
/* charge time to 100% */
/*============================================================================*/
package com.vandermudellc.smartbattery;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.TimeZone;

public class BatteryWidgetSmartFns
{
	public static final String LOG_TAG = "BatteryWidgetSmartFns";
	/* TODO - all read() and write() functions should be passed */
	/* the version number in the future */
	public static final int VERSION_NUMBER = 1;
	/* BatteryManager BATTERY_PLUGGED_AC = 1 */
	/* BatteryManager BATTERY_PLUGGED_USB = 2 */
	/* SO BATTERY_PLUGGED_SIZE is 3 */
	public static final String POWER_USB = "USB";
	public static final String POWER_AC = "AC";
	public static final String CHARGE_SIGN = "+";
	public static final String DISCHARGE_SIGN = "-";
	public static final String POWER_NONE = "--";
	public static final int BATTERY_UNPLUGGED = 0;
	public static final int BATTERY_PLUGGED_SIZE = 3;
	public static final String TIME_UNDEFINED = "??:??";
	static Context mContext = null;
	static BatteryFile datafile;
	/* Display values and strings */
	static int batteryPercentValue;
	static int batteryTimeValue;	/* time in seconds */
	/* Battery Info data */
	static long currtime;	/* battery data timestamp */
	static Calendar calendar;
	static int currday;		/* current day of year */
	static int currhour;	/* current hour */
	static int currminute;	/* current minute */
	static int pctlevel;	/* battery level in percent */
	static int status;		/* integer containing current status constant. */
	static int health;		/* integer containing current health constant. */
	static boolean present; /* boolean indicating whether a battery present. */
	static int level;		/* integer containing current battery level, */
							/* from 0 to {@link #EXTRA_SCALE}. */
	static int scale;		/* integer containing maximum battery level. */
	static int icon_small;	/* integer containing resource ID of the small */
							/* status bar icon indicating battery state. */
	static int power;		/* integer indicating whether device is plugged */
							/* into a power source; 0 means it is on battery, */
							/* other constants are different power sources. */
	static int voltage;		/* integer containing battery voltage level. */
	static int temperature; /* integer containing battery temperature. */
	String technology;		/* String describing technology of battery. */
	/* values for "status" field in ACTION_BATTERY_CHANGED Intent */
	/* BATTERY_STATUS_UNKNOWN, BATTERY_STATUS_CHARGING */
	/* BATTERY_STATUS_DISCHARGING, BATTERY_STATUS_NOT_CHARGING */
	/* BATTERY_STATUS_FULL */
	/* values for "health" field in ACTION_BATTERY_CHANGED Intent */
	/* BATTERY_HEALTH_UNKNOWN, BATTERY_HEALTH_GOOD */
	/* BATTERY_HEALTH_OVERHEAT, BATTERY_HEALTH_DEAD */
	/* BATTERY_HEALTH_OVER_VOLTAGE, BATTERY_HEALTH_UNSPECIFIED_FAILURE */
	/* values for "power" field in ACTION_BATTERY_CHANGED intent. */
	/* These must be powers of 2. */
	/* BATTERY_PLUGGED_AC, BATTERY_PLUGGED_USB */
	static int runstate;	/* integer containing the unit run state */
	/* values for the runstate field in BatteryWidgetSmartFns */
	public static final int RUN_STATE_OFF = 0;
	public static final int RUN_STATE_SLEEP = 1;
	public static final int RUN_STATE_FULL = 2;
	public static final int DISPLAY_LINES = 5;
	public static boolean debug_start_flag = true;

	public BatteryWidgetSmartFns(/*Context context*/)
	{
		calendar = Calendar.getInstance();
	}

	public void BatteryWidgetSetContext(Context context)
	{
		mContext = context;	/* needed for file read/write */
		datafile = new BatteryFile(context);
		if(mContext != null)
		{
			datafile.LoadFile(); /* Done only once at startup */
		}
		if	(datafile.BatteryMinuteGetDay() == 0)
		{
			currday = calendar.get(Calendar.DAY_OF_YEAR);
			datafile.BatteryMinuteSetDay(currday);
		}
	}

	public void UpdateStats(int v_status, int v_health, boolean v_present,
		int v_level, int v_scale, int v_icon_small, int v_power,
		int v_voltage, int v_temperature, String v_technology,
		boolean v_screenon)
	{
		status = v_status;
		health = v_health;
		present = v_present;
		level = v_level;
		scale = v_scale;
		icon_small = v_icon_small;
		power = v_power;
		voltage = v_voltage;
		temperature = v_temperature;
		technology = v_technology;
		if	(level >= 0 && scale > 0)
		{
			pctlevel = (level * 100) / scale;
		}
		if	(v_screenon == true)
		{
			runstate = RUN_STATE_FULL;
		}
		else
		{
			runstate = RUN_STATE_SLEEP;
		}
		currtime = System.currentTimeMillis();
		calendar.setTimeInMillis(currtime);
		calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
		/* We do not check if previous year is leap year */
		/* This will cause us to skip Dec 31 of leap year is unit is */
		/* turned off on that day */
		currday = calendar.get(Calendar.DAY_OF_YEAR);
		currhour = calendar.get(Calendar.HOUR_OF_DAY);
		currminute = calendar.get(Calendar.MINUTE);
		/* debug_update_1sec(); */
		if(mContext != null)
		{
			datafile.ProcessEntry(currday, currhour, currminute,
					pctlevel, status, health, power, runstate);
			/* Could be done only once a day, but then we'd lose data */
			datafile.SaveFile();
		}
		/* debug_output(); */
	}

	public void debug_update_1sec()
	{
		currminute = calendar.get(Calendar.SECOND);
		currhour = (calendar.get(Calendar.MINUTE) +
		(calendar.get(Calendar.HOUR_OF_DAY) * 60) +
		(calendar.get(Calendar.DAY_OF_YEAR) * 60 * 24)) % 24;
		currday = ((calendar.get(Calendar.MINUTE) +
		(calendar.get(Calendar.HOUR_OF_DAY) * 60) +
		(calendar.get(Calendar.DAY_OF_YEAR) * 60 * 24)) / 24) % 365;
		if	(currhour >= 1 && currhour <= 11)
		{
			/* charging */
			if	((currday % 2) == 0)
			{
			power = BatteryManager.BATTERY_PLUGGED_AC;
			}
			else
			{
			power = BatteryManager.BATTERY_PLUGGED_USB;
			}
			pctlevel = (currhour - 1) * 10;
			if	((currminute % 2) == 0)
			{
				runstate = RUN_STATE_SLEEP;
			}
			else
			{
				runstate = RUN_STATE_FULL;
			}
		}
		else
		if	(currhour >= 12 && currhour <= 22)
		{
			/* discharging */
			power = BatteryWidgetSmartFns.BATTERY_UNPLUGGED;
			pctlevel = (22 - currhour) * 10;
			if	((currminute % 2) == 0)
			{
				runstate = RUN_STATE_SLEEP;
			}
			else
			{
				runstate = RUN_STATE_FULL;
			}
		}
		else
		{
			/* off */
			pctlevel = 0;
			runstate = RUN_STATE_OFF;
		}
		String debug_status_line =
			currday + ":" + currhour + ":" + currminute;
		SharedPreferences shared_prefs =
			PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = shared_prefs.edit();
		editor.putString("debug_status", debug_status_line);
		editor.commit();
	}

	public void debug_output()
	{
		try
		{
			File root = Environment.getExternalStorageDirectory();
			if (root.canWrite())
			{
				File debug_file = new File(root, "SmartBatteryData.txt");
				FileWriter debug_writer = new FileWriter(debug_file, true);
				BufferedWriter out = new BufferedWriter(debug_writer);
				if	(debug_start_flag == true)
				{
					out.write("Program Start\n");
					out.flush();
					debug_start_flag = false;
				}
				String out_line =
					currday + "," + currhour + "," + currminute + "," +
					status + "," + health + "," + present + "," +
					level + "," + scale + "," + power + "," +
					voltage + "," + temperature + "," +
					pctlevel + "," + runstate + "," + technology + "\n";
				out.write(out_line);
				out.flush();
				out.close();
			}
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "Could not write file " + e.getMessage());
		}

	}

	public int BatteryPercentValue()
	{
		return(pctlevel);
	}

	public String BatteryPercentString()
	{
		String percentstring;
		batteryPercentValue = pctlevel;
		percentstring = String.valueOf(batteryPercentValue) + "%";
		return(percentstring);
	}

	public int BatteryPercentIndex()
	{
		int percentindex;
		percentindex = (pctlevel + 4) / 5;
		return(percentindex);
	}

	public String BatteryTime(int discharge_level, int charge_level)
	{
		String timestring;
		batteryTimeValue =
			datafile.BatteryTimeMins(discharge_level, charge_level);
		if	(batteryTimeValue >= 0)
		{
			int batteryTimeHours = batteryTimeValue / 60;
			if	(batteryTimeHours > 99)
			{
				batteryTimeHours = 99;
			}
			int batteryTimeMins = batteryTimeValue % 60;
			timestring = String.valueOf(batteryTimeHours) + ":" +
				String.format("%02d", batteryTimeMins);
		}
		else
		{
			/* This should never happen */
			timestring = TIME_UNDEFINED;
		}
		return(timestring);
	}

	public String BatterySourceString()
	{
		int power;
		String powerstring;
		power = datafile.BatteryPower();
		if	(power == BatteryManager.BATTERY_PLUGGED_AC)
		{
			powerstring = POWER_AC;
		}
		else
		if	(power == BatteryManager.BATTERY_PLUGGED_USB)
		{
			powerstring = POWER_USB;
		}
		else
		{
			powerstring = POWER_NONE;
		}
		return(powerstring);
	}
}

class MovingAverage
{
	public static final int MOVINGAVERAGECHARSIZE = 3;
	private static int MovingAverageCount = 16; /* Default - can be changed */
	int acc;		/* Three byte value */
	int cnt;		/* One byte value */
	int avg;		/* Two byte value */

	public MovingAverage()
	{
		initialize();
	}

	public void initialize()
	{
		acc = 0;
		cnt = 0;
		avg = 0;
	}

	public void SetMovingAverageCount(int value)
	{
		MovingAverageCount = value;
	}

	public void store(int x)
	{
		acc += x;
		if	(cnt < MovingAverageCount)
		{
			cnt++;
		}
		else
		{
			acc -= avg;
		}
		avg = acc / cnt;
	}

	public int get()
	{
		return(avg);
	}

	public void read(char readarray[])
	{
		acc = (readarray[0] << 8) | (readarray[1] & 0xFF);	/* Three bytes */
		cnt = (readarray[1] >> 8) & 0xFF;					/* One byte */
		avg = readarray[2];									/* Two bytes */
	}

	public void write(char writearray[])
	{
		writearray[0] = (char) (((acc >> 8) & 0xFFFF));
		writearray[1] = (char) ((acc & 0xFF) | (cnt << 8));
		writearray[2] = (char) (avg);
	}
}

class BatteryMinute
{
	public static final int BATTERYMINUTECHARSIZE = 2; /* in 16 bit chars */
	public int pctlevel;	/* byte battery level in percent */
	public int status;		/* hexdigit containing current status constant */
	public int health;		/* hexdigit containing current health constant */
	public int power;		/* hexdigit indicating power source */
	public int runstate;	/* hexdigit indicating runstate */
	public int smoothpower;	/* smoothing variable */
	public int smoothlevel;	/* smoothing variable */
	public boolean smoothzero;	/* true == skip this line */

	public BatteryMinute()
	{
		initialize();
	}

	public void initialize()
	{
		pctlevel = 0;
		status = 0;
		health = 0;
		power = 0;
		runstate = 0;
	}

	public void copy(BatteryMinute x)
	{
		pctlevel = x.pctlevel;
		status = x.status;
		health = x.health;
		power = x.power;
		runstate = x.runstate;
	}

	public void set(int v_pctlevel, int v_status, int v_health, int v_power,
		int v_runstate)
	{
		pctlevel = v_pctlevel;
		status = v_status;
		health = v_health;
		power = v_power;
		runstate = v_runstate;
	}

	public void read(char readarray[])
	{
		pctlevel = (int) readarray[0];
		status = readarray[1] & 0x0F;
		health = (readarray[1] >> 4) & 0x0F;
		power = (readarray[1] >> 8) & 0x0F;
		runstate = (readarray[1] >> 12) & 0x0F;
	}

	public void write(char writearray[])
	{
		writearray[0] = (char) pctlevel;
		writearray[1] =
			(char) ((runstate << 12) | (power << 8) | (health << 4) | status);
	}
}

/* The first day's worth of data is the older data. This data is processed */
/* when the day changes. The second day's data is today's data being */
/* currently stored. */
class BatteryMinuteData
{
	public static final int BATTERYMINUTESIZE = 2 * 24 * 60;
	public static final int BATTERYMINUTEDAYSIZE = 24 * 60;
	public static final int RUN_STATE_SMOOTH_SIZE = 30; /* 30 minutes */
	public static final int LEVEL_SMOOTH_SIZE = 15; /* 15 minutes */
	public static final int ZEROSKIP = 6; /* skip 6 zeros */
	BatteryMinute[] BatteryMinuteArray;
	int storeday;	/* Day of month of first day's array */
	private static int datasize;
	private static char[] databuffer;
	private static char[] dataentry;

	public BatteryMinuteData()
	{
		BatteryMinuteArray = new BatteryMinute[BATTERYMINUTESIZE];
		datasize = BATTERYMINUTESIZE * BatteryMinute.BATTERYMINUTECHARSIZE;
		datasize++; /* for storeday */
		databuffer = new char[datasize];
		dataentry = new char[BatteryMinute.BATTERYMINUTECHARSIZE];
		int i;
		for (i = 0; i < BATTERYMINUTESIZE; i++)
		{
			BatteryMinuteArray[i] = new BatteryMinute();
		}
	}

	public void initialize()
	{
		int i;
		for (i = 0; i < BATTERYMINUTESIZE; i++)
		{
			BatteryMinuteArray[i].initialize();
		}
	}

	public void BatteryMinuteSetDay(int v_day)
	{
		storeday = v_day;
	}

	public int BatteryMinuteGetDay()
	{
		return(storeday);
	}

	public void read(InputStreamReader rdfile) throws IOException
	{
		rdfile.read(databuffer, 0, datasize);
		int offset;
		offset = 0;
		storeday = databuffer[0];
		offset++;
		int i;
		for (i = 0; i < BATTERYMINUTESIZE; i++)
		{
			System.arraycopy(databuffer, offset, dataentry, 0,
				BatteryMinute.BATTERYMINUTECHARSIZE);
			offset += BatteryMinute.BATTERYMINUTECHARSIZE;
			BatteryMinuteArray[i].read(dataentry);
		}
	}

	public void write(OutputStreamWriter wrfile) throws IOException
	{
		int offset;
		offset = 0;
		databuffer[0] = (char) storeday;
		offset++;
		int i;
		for (i = 0; i < BATTERYMINUTESIZE; i++)
		{
			BatteryMinuteArray[i].write(dataentry);
			System.arraycopy(dataentry, 0, databuffer, offset,
				BatteryMinute.BATTERYMINUTECHARSIZE);
			offset += BatteryMinute.BATTERYMINUTECHARSIZE;
		}
		wrfile.write(databuffer, 0, datasize);
	}

	/* Remove any short times where data was not collected and */
	/* the start and end minute entries are identical - fill gap */
	/* with the start/end values */
	/* TODO Adjust power levels that go up during discharge or down */
	/* during charge */
	public void smooth()
	{
		smooth_init();
		smooth_oneline();
		smooth_zeros();
		smooth_power();
		smooth_level();
	}

	/* Initialize smooth fields */
	public void smooth_init()
	{
		int i;
		for (i = 0; i < BATTERYMINUTESIZE; i++)
		{
			BatteryMinuteArray[i].smoothzero = false;
			BatteryMinuteArray[i].smoothpower = 0;
			BatteryMinuteArray[i].smoothlevel = 0;
		}
	}

	/* Replace ABA by AAA */
	public void smooth_oneline()
	{
		int i;
		for (i = 1; i < (BATTERYMINUTESIZE - 1); i++)
		{
			if	((BatteryMinuteArray[i - 1].pctlevel ==
				BatteryMinuteArray[i + 1].pctlevel) &&
				(BatteryMinuteArray[i - 1].status ==
				BatteryMinuteArray[i + 1].status) &&
				(BatteryMinuteArray[i - 1].health ==
				BatteryMinuteArray[i + 1].health) &&
				(BatteryMinuteArray[i - 1].power ==
				BatteryMinuteArray[i + 1].power) &&
				(BatteryMinuteArray[i - 1].runstate ==
				BatteryMinuteArray[i + 1].runstate) &&
				((BatteryMinuteArray[i - 1].pctlevel !=
				BatteryMinuteArray[i].pctlevel) ||
				(BatteryMinuteArray[i - 1].status !=
				BatteryMinuteArray[i].status) ||
				(BatteryMinuteArray[i - 1].health !=
				BatteryMinuteArray[i].health) ||
				(BatteryMinuteArray[i - 1].power !=
				BatteryMinuteArray[i].power) ||
				(BatteryMinuteArray[i - 1].runstate !=
				BatteryMinuteArray[i].runstate)))
			{
				BatteryMinuteArray[i].pctlevel =
					BatteryMinuteArray[i - 1].pctlevel;
				BatteryMinuteArray[i].status =
					BatteryMinuteArray[i - 1].status;
				BatteryMinuteArray[i].health =
					BatteryMinuteArray[i - 1].health;
				BatteryMinuteArray[i].power =
					BatteryMinuteArray[i - 1].power;
				BatteryMinuteArray[i].runstate =
					BatteryMinuteArray[i - 1].runstate;
			}
		}
	}

	/* Flag short runs of zeros */
	public void smooth_zeros()
	{
		int i;
		int j;
		boolean startflag;
		int zerocnt;
		startflag = false;
		zerocnt = 0;
		for (i = 0; i < BATTERYMINUTESIZE; i++)
		{
				BatteryMinuteArray[i].smoothzero = false;
		}
		for (i = 0; i < BATTERYMINUTESIZE; i++)
		{
			if	((BatteryMinuteArray[i].pctlevel == 0) &&
				(BatteryMinuteArray[i].status == 0) &&
				(BatteryMinuteArray[i].health == 0) &&
				(BatteryMinuteArray[i].power == 0) &&
				(BatteryMinuteArray[i].runstate == 0))
			{
				zerocnt++;
			}
			else
			{
				if	(startflag == true && zerocnt > 0 && zerocnt <= ZEROSKIP)
				{
					for	(j = (i - zerocnt); j < i; j++)
					{
						BatteryMinuteArray[j].smoothzero = true;
					}
				}
				zerocnt = 0;
				startflag = true;
			}
		}
	}

	/* Flag short power intervals */
	/* separate out data where power is unplugged or plugged */
	/* this should be the same as status is charging or discharging */
	public void smooth_power()
	{
		int i;
		int j;
		int midindex;
		int addpower;
		int subpower;
		int maxpower;
		int[] SmoothStatusArray;
		SmoothStatusArray = new int[BatteryWidgetSmartFns.BATTERY_PLUGGED_SIZE];
		for (i = 1; i < BatteryWidgetSmartFns.BATTERY_PLUGGED_SIZE; i++)
		{
			SmoothStatusArray[i] = 0;
		}
		maxpower = 0;
		for (i = 0; i < BATTERYMINUTESIZE; i++)
		{
			addpower = -1;
			if	(i <= (BATTERYMINUTESIZE - RUN_STATE_SMOOTH_SIZE))
			{
				addpower = BatteryMinuteArray[i].power;
				SmoothStatusArray[addpower]++;
			}
			subpower = -1;
			if	(i >= RUN_STATE_SMOOTH_SIZE)
			{
				subpower = BatteryMinuteArray[i - RUN_STATE_SMOOTH_SIZE].power;
				SmoothStatusArray[subpower]--;
			}
			if	((i >= RUN_STATE_SMOOTH_SIZE) &&
				(i <= (BATTERYMINUTESIZE - RUN_STATE_SMOOTH_SIZE)))
			{
				if	((i == RUN_STATE_SMOOTH_SIZE) || (addpower != subpower))
				{
					maxpower = 0;
					for	(j = 1;
						j < BatteryWidgetSmartFns.BATTERY_PLUGGED_SIZE; j++)
					{
						if	(SmoothStatusArray[maxpower] < SmoothStatusArray[j])
						{
							maxpower = j;
						}
					}
				}
				midindex = i - (RUN_STATE_SMOOTH_SIZE / 2);
				if	((BatteryMinuteArray[midindex].power != maxpower) &&
					(SmoothStatusArray[BatteryMinuteArray[midindex].power] <
					SmoothStatusArray[maxpower]))
				{
					BatteryMinuteArray[midindex].smoothpower = maxpower;
				}
				else
				{
					BatteryMinuteArray[midindex].smoothpower = -1;
				}
			}
		}
	}

	/* Flag short level intervals */
	/* separate out data where level data is squirrely */
	public void smooth_level()
	{
		int i;
		int j;
		int arrysize;
		int midindex;
		int addlevel;
		int sublevel;
		int maxlevel;
		int[] SmoothStatusArray;
		arrysize = 101;
		SmoothStatusArray = new int[arrysize];
		for (i = 0; i < arrysize; i++)
		{
			SmoothStatusArray[i] = 0;
		}
		maxlevel = 0;
		for (i = 0; i < BATTERYMINUTESIZE; i++)
		{
			addlevel = -1;
			if	(i <= (BATTERYMINUTESIZE - LEVEL_SMOOTH_SIZE))
			{
				addlevel = BatteryMinuteArray[i].pctlevel;
				SmoothStatusArray[addlevel]++;
			}
			sublevel = -1;
			if	(i >= LEVEL_SMOOTH_SIZE)
			{
				sublevel = BatteryMinuteArray[i - LEVEL_SMOOTH_SIZE].pctlevel;
				SmoothStatusArray[sublevel]--;
			}
			if	((i >= LEVEL_SMOOTH_SIZE) &&
				(i <= (BATTERYMINUTESIZE - LEVEL_SMOOTH_SIZE)))
			{
				if	((i == LEVEL_SMOOTH_SIZE) || (addlevel != sublevel))
				{
					maxlevel = 0;
					for	(j = 0; j < arrysize; j++)
					{
						if	(SmoothStatusArray[maxlevel] < SmoothStatusArray[j])
						{
							maxlevel = j;
						}
					}
				}
				midindex = i - (LEVEL_SMOOTH_SIZE / 2);
				if	((BatteryMinuteArray[midindex].pctlevel != maxlevel) &&
					(SmoothStatusArray[BatteryMinuteArray[midindex].pctlevel] <
					SmoothStatusArray[maxlevel]))
				{
					BatteryMinuteArray[midindex].smoothlevel = maxlevel;
				}
				else
				{
					BatteryMinuteArray[midindex].smoothlevel = -1;
				}
			}
		}
	}

	public void shift(int newday)
	{
		int i;
		for (i = 0; i < BATTERYMINUTEDAYSIZE; i++)
		{
			BatteryMinuteArray[i].copy(
				BatteryMinuteArray[i + BATTERYMINUTEDAYSIZE]);
			BatteryMinuteArray[i + BATTERYMINUTEDAYSIZE].initialize();
		}
		storeday = newday;
	}

	/* Store new data in second bank of day values */
	public void ProcessEntry(int procday, int prochour, int procminute,
		int pctlevel, int status, int health, int power, int runstate)
	{
		int minute = (prochour * 60) + procminute;
		if	(procday >= (storeday + 1))
		{
			storeday = procday;
		}
		/* store data in second day */
		minute += BATTERYMINUTEDAYSIZE;
		BatteryMinuteArray[minute].set(
			pctlevel, status, health, power, runstate);
	}
}

/* For a given discharge level, add to the moving average */
class BatterySummaryLevelDischarge
{
	public static final int BATTERYSUMMARYLEVELDISCHARGECHARSIZE =
		MovingAverage.MOVINGAVERAGECHARSIZE;
	MovingAverage on; /* moving average of total discharge time for level */

	public BatterySummaryLevelDischarge()
	{
		on = new MovingAverage();
	}

	public void initialize()
	{
		on.initialize();
	}

	public void read(char readarray[])
	{
		on.read(readarray);
	}

	public void write(char writearray[])
	{
		on.write(writearray);
	}

	public void store(int x)
	{
		on.store(x);
	}
}

/* Time for charging is simply the number of minutes at that level */
class BatterySummaryLevelCharge
{
	MovingAverage usb;
	MovingAverage ac;
	public static final int BATTERYSUMMARYLEVELCHARGECHARSIZE =
		MovingAverage.MOVINGAVERAGECHARSIZE +
		MovingAverage.MOVINGAVERAGECHARSIZE;
	private static final int DATAENTRYSIZE = MovingAverage.MOVINGAVERAGECHARSIZE;
	private static char[] dataentry;

	public BatterySummaryLevelCharge()
	{
		dataentry = new char[DATAENTRYSIZE];
		usb = new MovingAverage();
		ac = new MovingAverage();
	}

	public void initialize()
	{
		usb.initialize();
		ac.initialize();
	}

	public void read(char readarray[])
	{
		int offset;
		offset = 0;
		System.arraycopy(readarray, offset, dataentry, 0, DATAENTRYSIZE);
		offset += DATAENTRYSIZE;
		usb.read(dataentry);
		System.arraycopy(readarray, offset, dataentry, 0, DATAENTRYSIZE);
		ac.read(dataentry);
	}

	public void write(char writearray[])
	{
		int offset;
		offset = 0;
		usb.write(dataentry);
		System.arraycopy(dataentry, 0, writearray, offset, DATAENTRYSIZE);
		offset += DATAENTRYSIZE;
		ac.write(dataentry);
		System.arraycopy(dataentry, 0, writearray, offset, DATAENTRYSIZE);
	}

	public void storeusb(int x)
	{
		usb.store(x);
	}

	public void storeac(int x)
	{
		ac.store(x);
	}
}

class BatterySummaryLevel
{
	BatterySummaryLevelDischarge discharge;
	BatterySummaryLevelCharge charge;
	public static final int BATTERYSUMMARYLEVELCHARSIZE =
		BatterySummaryLevelDischarge.BATTERYSUMMARYLEVELDISCHARGECHARSIZE +
		BatterySummaryLevelCharge.BATTERYSUMMARYLEVELCHARGECHARSIZE;
	private static final int DATAENTRYDISCHARGESIZE =
		BatterySummaryLevelDischarge.BATTERYSUMMARYLEVELDISCHARGECHARSIZE;
	private static final int DATAENTRYCHARGESIZE =
		BatterySummaryLevelCharge.BATTERYSUMMARYLEVELCHARGECHARSIZE;
	private static char[] dataentrydischarge;
	private static char[] dataentrycharge;

	public BatterySummaryLevel()
	{
		dataentrydischarge = new char[DATAENTRYDISCHARGESIZE];
		dataentrycharge = new char[DATAENTRYCHARGESIZE];
		discharge = new BatterySummaryLevelDischarge();
		charge = new BatterySummaryLevelCharge();
	}

	public void initialize()
	{
		discharge.initialize();
		charge.initialize();
	}

	public void read(char readarray[])
	{
		int offset;
		offset = 0;
		System.arraycopy(readarray, offset, dataentrydischarge, 0,
			DATAENTRYDISCHARGESIZE);
		offset += DATAENTRYDISCHARGESIZE;
		discharge.read(dataentrydischarge);
		System.arraycopy(readarray, offset, dataentrycharge, 0,
			DATAENTRYCHARGESIZE);
		charge.read(dataentrycharge);
	}

	public void write(char writearray[])
	{
		int offset;
		offset = 0;
		discharge.write(dataentrydischarge);
		System.arraycopy(dataentrydischarge, 0, writearray, offset,
			DATAENTRYDISCHARGESIZE);
		offset += DATAENTRYDISCHARGESIZE;
		charge.write(dataentrycharge);
		System.arraycopy(dataentrycharge, 0, writearray, offset,
			DATAENTRYCHARGESIZE);
	}

	public void store(int power, int timefull, int timesleep, int timeoff)
	{
		/* save minutes full and sleep for discharge */
		/* just save time for charge - assumes charge time is constant */
		/* regardless of whether phone is full sleep or off */
		/* Ignore all off times */
		if	(power == BatteryManager.BATTERY_PLUGGED_AC)
		{
			charge.storeac(timefull + timesleep);
		}
		else
		if	(power == BatteryManager.BATTERY_PLUGGED_USB)
		{
			charge.storeusb(timefull + timesleep);
		}
		else
		{
			discharge.store(timefull + timesleep);
		}
	}
}

class BatterySummaryLevelData
{
	public static final int BATTERYSUMMARYLEVELSIZE = 21; /* 5% increments */
	BatterySummaryLevel[] BatterySummaryLevelArray;
	boolean LastChargingFlag = false;	/* True if unit charging during last */
										/* group of minutes processed */
	boolean LastDischargingFlag = false;/* True if unit on and discharging */
										/* during last group processed */
	/* private String debug_process_line; */
	/* private static Context mContext = null; */
	private static int datasize;
	private static char[] databuffer;
	private static char[] dataentry;

	public BatterySummaryLevelData(Context context)
	{
		/* mContext = context; */
		datasize = (BATTERYSUMMARYLEVELSIZE *
			BatterySummaryLevel.BATTERYSUMMARYLEVELCHARSIZE) + 1;
		databuffer = new char[datasize];
		dataentry = new char[BatterySummaryLevel.BATTERYSUMMARYLEVELCHARSIZE];
		BatterySummaryLevelArray =
			new BatterySummaryLevel[BATTERYSUMMARYLEVELSIZE];
		int i;
		for (i = 0; i < BATTERYSUMMARYLEVELSIZE; i++)
		{
			BatterySummaryLevelArray[i] = new BatterySummaryLevel();
		}
		LastChargingFlag = false;
		LastDischargingFlag = false;
		initialize();
	}

	public void initialize()
	{
		int i;
		for (i = 0; i < BATTERYSUMMARYLEVELSIZE; i++)
		{
			BatterySummaryLevelArray[i].initialize();
		}
		LastChargingFlag = false;
		LastDischargingFlag = false;
		InitializeData();
	}

	public void InitializeData()
	{
		int i;
		int[] ac_table =
	{19, 0, 19, 0, 11, 0, 20, 0, 15, 0, 15, 0, 12, 0, 26, 0, 26, 0, 20, 0, 0};
		int[] usb_table =
	{20, 0, 16, 0, 13, 0, 25, 0, 26, 0, 16, 0, 15, 0, 18, 0, 20, 0, 14, 0, 0};
		int[] dis_table =
	{0, 0, 66, 0, 60, 0, 60, 0, 50, 0, 60, 0, 53, 0, 60, 0, 60, 0, 100, 0, 15};
		for (i = 0; i < BATTERYSUMMARYLEVELSIZE; i++)
		{
			BatterySummaryLevelArray[i].store(
				BatteryWidgetSmartFns.BATTERY_UNPLUGGED, dis_table[i], 0 ,0);
		}
		for (i = 0; i < BATTERYSUMMARYLEVELSIZE; i++)
		{
			BatterySummaryLevelArray[i].store(
				BatteryManager.BATTERY_PLUGGED_USB, usb_table[i], 0 ,0);
		}
		for (i = 0; i < BATTERYSUMMARYLEVELSIZE; i++)
		{
			BatterySummaryLevelArray[i].store(
				BatteryManager.BATTERY_PLUGGED_AC, ac_table[i], 0 ,0);
		}
	}

	public void read(InputStreamReader rdfile) throws IOException
	{
		rdfile.read(databuffer, 0, datasize);
		if	((databuffer[0] & 0x01) != 0)
		{
			LastChargingFlag = true;
		}
		else
		{
			LastChargingFlag = false;
		}
		if	((databuffer[0] & 0x02) != 0)
		{
			LastDischargingFlag = true;
		}
		else
		{
			LastDischargingFlag = false;
		}
		int offset;
		offset = 1;
		int i;
		for (i = 0; i < BATTERYSUMMARYLEVELSIZE; i++)
		{
			System.arraycopy(databuffer, offset, dataentry, 0,
				BatterySummaryLevel.BATTERYSUMMARYLEVELCHARSIZE);
			offset += BatterySummaryLevel.BATTERYSUMMARYLEVELCHARSIZE;
			BatterySummaryLevelArray[i].read(dataentry);
		}
	}

	public void write(OutputStreamWriter wrfile) throws IOException
	{
		databuffer[0] = (char)
			((LastChargingFlag == true ? 1 : 0) |
			(LastDischargingFlag == true ? 2 : 0));
		int offset;
		offset = 1;
		int i;
		for (i = 0; i < BATTERYSUMMARYLEVELSIZE; i++)
		{
			BatterySummaryLevelArray[i].write(dataentry);
			System.arraycopy(dataentry, 0, databuffer, offset,
				BatterySummaryLevel.BATTERYSUMMARYLEVELCHARSIZE);
			offset += BatterySummaryLevel.BATTERYSUMMARYLEVELCHARSIZE;
		}
		wrfile.write(databuffer, 0, datasize);
	}

	public int pctvalue(int level)
	{
		int ret_valu;
		ret_valu = level * 5;
		return(ret_valu);
	}

	public int level(int pctvalue)
	{
		int ret_valu;
		if	(pctvalue == 0)
		{
			ret_valu = 0;
		}
		else
		if	(pctvalue == 100)
		{
			ret_valu = 20;
		}
		else
		{
			/* 1-5 = 1, 6-10 = 2, etc */
			ret_valu = ((pctvalue + 4) / 5);
		}
		return(ret_valu);
	}

	public int LevelTime(int power, int level)
	{
		int ret_valu;
		ret_valu = 0;
		if	(power == BatteryManager.BATTERY_PLUGGED_AC)
		{
			ret_valu = BatterySummaryLevelArray[level].charge.ac.avg;
		}
		else
		if	(power == BatteryManager.BATTERY_PLUGGED_USB)
		{
			ret_valu = BatterySummaryLevelArray[level].charge.usb.avg;
		}
		else
		{
			ret_valu = BatterySummaryLevelArray[level].discharge.on.avg;
		}
		return(ret_valu);
	}

	/* Compute the amount of time at each battery level */
	/* Charging and discharging */
	public void process(BatteryMinuteData minutedata)
	{
		/* debug_process_line = ""; */
		processcharge(minutedata);
		processdischarge(minutedata);
		/* debug_process(); */
	}

	/* Compute the amount of charge time at each battery level */
	public void processcharge(BatteryMinuteData minutedata)
	{
		final int CHARGESIZE = 60;
		final int CHARGELIMIT = 50;
		int i;
		int j;
		int k;
		int power;
		int pctlevel;
		int runstate;
		int smoothpower;
		int smoothlevel;
		int fullvalu;
		int chrgindx;
		int chrg_cnt;
		int chrgvalu;
		boolean regnflag;
		int regnindx;
		int pct_indx;
		int levelcnt;
		int totl_sum;
		int[] chrgtabl;
		int[] lvl_tabl;
		int[] fulltabl;
		int[] fullt_ac;
		int[] slp_t_ac;
		int[] chrgt_ac;
		int[] fulltusb;
		int[] slp_tusb;
		int[] chrgtusb;
		regnflag = false;
		chrgindx = 0;
		chrg_cnt = 0;
		chrgtabl = new int[CHARGESIZE];
		lvl_tabl = new int[CHARGESIZE];
		fulltabl = new int[CHARGESIZE];
		for (i = 0; i < CHARGESIZE; i++)
		{
			chrgtabl[i] = 0;
			lvl_tabl[i] = 0;
			fulltabl[i] = 0;
		}
		fullt_ac = new int[BATTERYSUMMARYLEVELSIZE];
		slp_t_ac = new int[BATTERYSUMMARYLEVELSIZE];
		chrgt_ac = new int[BATTERYSUMMARYLEVELSIZE];
		fulltusb = new int[BATTERYSUMMARYLEVELSIZE];
		slp_tusb = new int[BATTERYSUMMARYLEVELSIZE];
		chrgtusb = new int[BATTERYSUMMARYLEVELSIZE];
		for (i = -(CHARGESIZE / 2);
			i < BatteryMinuteData.BATTERYMINUTESIZE + (CHARGESIZE / 2); i++)
		{
			if	(i >= 0 && i < BatteryMinuteData.BATTERYMINUTESIZE)
			{
				runstate = minutedata.BatteryMinuteArray[i].runstate;
				power = minutedata.BatteryMinuteArray[i].power;
				smoothpower = minutedata.BatteryMinuteArray[i].smoothpower;
				pctlevel = minutedata.BatteryMinuteArray[i].pctlevel;
				smoothlevel = minutedata.BatteryMinuteArray[i].smoothlevel;
			}
			else
			if	(i < 0)
			{
				runstate = minutedata.BatteryMinuteArray[0].runstate;
				power = minutedata.BatteryMinuteArray[0].power;
				smoothpower = minutedata.BatteryMinuteArray[0].smoothpower;
				pctlevel = minutedata.BatteryMinuteArray[0].pctlevel;
				smoothlevel = minutedata.BatteryMinuteArray[0].smoothlevel;
			}
			else
			{
				j = BatteryMinuteData.BATTERYMINUTESIZE - 1;
				runstate = minutedata.BatteryMinuteArray[j].runstate;
				power = minutedata.BatteryMinuteArray[j].power;
				smoothpower = minutedata.BatteryMinuteArray[j].smoothpower;
				pctlevel = minutedata.BatteryMinuteArray[j].pctlevel;
				smoothlevel = minutedata.BatteryMinuteArray[0].smoothlevel;
			}
			if	(smoothpower != -1 && smoothpower != 0)
			{
				power = smoothpower;
			}
			if	(smoothlevel != -1 && smoothlevel != 0)
			{
				pctlevel = smoothlevel;
			}
			if	(runstate == BatteryWidgetSmartFns.RUN_STATE_FULL)
			{
				fullvalu = 1;
			}
			else
			{
				fullvalu = 0;
			}
			if	((power == 0) && (pctlevel > 0))
			{
				chrgvalu = 0;
			}
			else
			{
				chrgvalu = 1;
			}
			chrg_cnt -= chrgtabl[chrgindx];
			chrgtabl[chrgindx] = chrgvalu;
			lvl_tabl[chrgindx] = level(pctlevel);
			fulltabl[chrgindx] = fullvalu;
			chrg_cnt += chrgtabl[chrgindx];
			chrgindx++;
			if	(chrgindx >= CHARGESIZE)
			{
				chrgindx = 0;
			}
			if	(i > (CHARGESIZE / 2))
			{
				if	(chrg_cnt >= CHARGELIMIT)
				{
					if	(regnflag == false)
					{
						for	(j = 0; j < BATTERYSUMMARYLEVELSIZE; j++)
						{
							fullt_ac[j] = 0;
							slp_t_ac[j] = 0;
							chrgt_ac[j] = 0;
							fulltusb[j] = 0;
							slp_tusb[j] = 0;
							chrgtusb[j] = 0;
						}
					}
					regnindx = chrgindx - (CHARGESIZE / 2);
					if	(regnindx < 0)
					{
						regnindx += CHARGESIZE;
					}
					pct_indx = lvl_tabl[regnindx];
					if	(chrgtabl[regnindx] == 0)
					{
						if	(power == BatteryManager.BATTERY_PLUGGED_AC)
						{
							chrgt_ac[pct_indx]++;
						}
						if	(power == BatteryManager.BATTERY_PLUGGED_USB)
						{
							chrgtusb[pct_indx]++;
						}
					}
					else
					if	(fulltabl[regnindx] == 1)
					{
						if	(power == BatteryManager.BATTERY_PLUGGED_AC)
						{
							fullt_ac[pct_indx]++;
						}
						if	(power == BatteryManager.BATTERY_PLUGGED_USB)
						{
							fulltusb[pct_indx]++;
						}
					}
					else
					{
						if	(power == BatteryManager.BATTERY_PLUGGED_AC)
						{
							slp_t_ac[pct_indx]++;
						}
						if	(power == BatteryManager.BATTERY_PLUGGED_USB)
						{
							slp_tusb[pct_indx]++;
						}
					}
					regnflag = true;
				}
				else
				{
					if	(regnflag == true)
					{
						levelcnt = 0;
						for	(k = 0; k < BATTERYSUMMARYLEVELSIZE; k++)
						{
							totl_sum = fullt_ac[k] + slp_t_ac[k] + chrgt_ac[k];
							if(totl_sum != 0)
							{
								levelcnt++;
							}
						}
						if	(levelcnt >= 3)
						{
							/* No time for 100% */
							for	(k = 0; k < BATTERYSUMMARYLEVELSIZE - 1; k++)
							{
								totl_sum = fullt_ac[k] + slp_t_ac[k] +
									chrgt_ac[k];
								if(totl_sum != 0)
								{
									/* if	(k == 10) */
									/* { */
										/* debug_process_line += "[" + */
						/* BatterySummaryLevelArray[k].charge.ac.acc + ":" + */
						/* BatterySummaryLevelArray[k].charge.ac.cnt + ":" + */
						/* BatterySummaryLevelArray[k].charge.ac.avg + "]" + */
											/* fullt_ac[k] + "+" + slp_t_ac[k]; */
									/* } */
									/* store values in data table */
									BatterySummaryLevelArray[k].store(
										BatteryManager.BATTERY_PLUGGED_AC,
										fullt_ac[k], slp_t_ac[k], chrgt_ac[k]);
									/* if	(k == 10) */
									/* { */
										/* debug_process_line += "[" + */
						/* BatterySummaryLevelArray[k].charge.ac.acc + ":" + */
						/* BatterySummaryLevelArray[k].charge.ac.cnt + ":" + */
						/* BatterySummaryLevelArray[k].charge.ac.avg + "]"; */
									/* } */
								}
							}
						}
						levelcnt = 0;
						for	(k = 0; k < BATTERYSUMMARYLEVELSIZE; k++)
						{
							totl_sum = fulltusb[k] + slp_tusb[k] + chrgtusb[k];
							if(totl_sum != 0)
							{
								levelcnt++;
							}
						}
						if	(levelcnt >= 3)
						{
							/* No time for 100% */
							for	(k = 0; k < BATTERYSUMMARYLEVELSIZE - 1; k++)
							{
								totl_sum = fulltusb[k] + slp_tusb[k] +
									chrgtusb[k];
								if(totl_sum != 0)
								{
									/* if	(k == 10) */
									/* { */
										/* debug_process_line += "[" + */
						/* BatterySummaryLevelArray[k].charge.usb.acc + ":" + */
						/* BatterySummaryLevelArray[k].charge.usb.cnt + ":" + */
						/* BatterySummaryLevelArray[k].charge.usb.avg + "]" + */
											/* fulltusb[k] + "+" + slp_tusb[k]; */
									/* } */
									/* store values in data table */
									BatterySummaryLevelArray[k].store(
										BatteryManager.BATTERY_PLUGGED_USB,
										fulltusb[k], slp_tusb[k], chrgtusb[k]);
									/* if	(k == 10) */
									/* { */
										/* debug_process_line += "[" + */
						/* BatterySummaryLevelArray[k].charge.usb.acc + ":" + */
						/* BatterySummaryLevelArray[k].charge.usb.cnt + ":" + */
						/* BatterySummaryLevelArray[k].charge.usb.avg + "]"; */
									/* } */
								}
							}
						}
					}
					regnflag = false;
				}
			}
		}
	}

	/* Compute the amount of discharge time at each battery level */
	public void processdischarge(BatteryMinuteData minutedata)
	{
		final int DISCHARGESIZE = 60;
		final int DISCHARGELIMIT = 50;
		int i;
		int j;
		int k;
		int power;
		int pctlevel;
		int runstate;
		int smoothpower;
		int smoothlevel;
		int fullvalu;
		int dchgindx;
		int dchg_cnt;
		int dchgvalu;
		boolean regnflag;
		int regnindx;
		int pct_indx;
		int levelcnt;
		int totl_sum;
		int[] dchgtabl;
		int[] lvl_tabl;
		int[] fulltabl;
		int[] fulltime;
		int[] slp_time;
		int[] chrgtime;
		regnflag = false;
		dchgindx = 0;
		dchg_cnt = 0;
		dchgtabl = new int[DISCHARGESIZE];
		lvl_tabl = new int[DISCHARGESIZE];
		fulltabl = new int[DISCHARGESIZE];
		for (i = 0; i < DISCHARGESIZE; i++)
		{
			dchgtabl[i] = 0;
			lvl_tabl[i] = 0;
			fulltabl[i] = 0;
		}
		fulltime = new int[BATTERYSUMMARYLEVELSIZE];
		slp_time = new int[BATTERYSUMMARYLEVELSIZE];
		chrgtime = new int[BATTERYSUMMARYLEVELSIZE];
		for (i = -(DISCHARGESIZE / 2);
			i < BatteryMinuteData.BATTERYMINUTESIZE + (DISCHARGESIZE / 2); i++)
		{
			if	(i >= 0 && i < BatteryMinuteData.BATTERYMINUTESIZE)
			{
				runstate = minutedata.BatteryMinuteArray[i].runstate;
				power = minutedata.BatteryMinuteArray[i].power;
				smoothpower = minutedata.BatteryMinuteArray[i].smoothpower;
				pctlevel = minutedata.BatteryMinuteArray[i].pctlevel;
				smoothlevel = minutedata.BatteryMinuteArray[i].smoothlevel;
			}
			else
			if	(i < 0)
			{
				runstate = minutedata.BatteryMinuteArray[0].runstate;
				power = minutedata.BatteryMinuteArray[0].power;
				smoothpower = minutedata.BatteryMinuteArray[0].smoothpower;
				pctlevel = minutedata.BatteryMinuteArray[0].pctlevel;
				smoothlevel = minutedata.BatteryMinuteArray[0].smoothlevel;
			}
			else
			{
				j = BatteryMinuteData.BATTERYMINUTESIZE - 1;
				runstate = minutedata.BatteryMinuteArray[j].runstate;
				power = minutedata.BatteryMinuteArray[j].power;
				smoothpower = minutedata.BatteryMinuteArray[j].smoothpower;
				pctlevel = minutedata.BatteryMinuteArray[j].pctlevel;
				smoothlevel = minutedata.BatteryMinuteArray[0].smoothlevel;
			}
			if	(smoothpower != -1 && smoothpower != 0)
			{
				power = smoothpower;
			}
			if	(smoothlevel != -1 && smoothlevel != 0)
			{
				pctlevel = smoothlevel;
			}
			if	(runstate == BatteryWidgetSmartFns.RUN_STATE_FULL)
			{
				fullvalu = 1;
			}
			else
			{
				fullvalu = 0;
			}
			if	((power == 0) && (pctlevel > 0))
			{
				dchgvalu = 1;
			}
			else
			{
				dchgvalu = 0;
			}
			dchg_cnt -= dchgtabl[dchgindx];
			dchgtabl[dchgindx] = dchgvalu;
			lvl_tabl[dchgindx] = level(pctlevel);
			fulltabl[dchgindx] = fullvalu;
			dchg_cnt += dchgtabl[dchgindx];
			dchgindx++;
			if	(dchgindx >= DISCHARGESIZE)
			{
				dchgindx = 0;
			}
			if	(i > (DISCHARGESIZE / 2))
			{
				if	(dchg_cnt >= DISCHARGELIMIT)
				{
					if	(regnflag == false)
					{
						for	(j = 0; j < BATTERYSUMMARYLEVELSIZE; j++)
						{
							fulltime[j] = 0;
							slp_time[j] = 0;
							chrgtime[j] = 0;
						}
					}
					regnindx = dchgindx - (DISCHARGESIZE / 2);
					if	(regnindx < 0)
					{
						regnindx += DISCHARGESIZE;
					}
					pct_indx = lvl_tabl[regnindx];
					if	(dchgtabl[regnindx] == 0)
					{
						chrgtime[pct_indx]++;
					}
					else
					if	(fulltabl[regnindx] == 1)
					{
						fulltime[pct_indx]++;
					}
					else
					{
						slp_time[pct_indx]++;
					}
					regnflag = true;
				}
				else
				{
					if	(regnflag == true)
					{
						levelcnt = 0;
						for	(k = 0; k < BATTERYSUMMARYLEVELSIZE; k++)
						{
							totl_sum = fulltime[k] + slp_time[k] + chrgtime[k];
							if(totl_sum != 0)
							{
								levelcnt++;
							}
						}
						if	(levelcnt >= 3)
						{
							/* No time for 0% */
							for	(k = 1; k < BATTERYSUMMARYLEVELSIZE; k++)
							{
								totl_sum = fulltime[k] + slp_time[k] +
									chrgtime[k];
								if(totl_sum != 0)
								{
									/* if	(k == 10) */
									/* { */
										/* debug_process_line += "[" + */
					/* BatterySummaryLevelArray[k].discharge.on.acc + ":" + */
					/* BatterySummaryLevelArray[k].discharge.on.cnt + ":" + */
					/* BatterySummaryLevelArray[k].discharge.on.avg + "]" + */
											/* fulltime[k] + "+" + slp_time[k]; */
									/* } */
									/* store values in data table */
									BatterySummaryLevelArray[k].store(
										BatteryWidgetSmartFns.BATTERY_UNPLUGGED,
										fulltime[k], slp_time[k], chrgtime[k]);
									/* if	(k == 10) */
									/* { */
										/* debug_process_line += "[" + */
					/* BatterySummaryLevelArray[k].discharge.on.acc + ":" + */
					/* BatterySummaryLevelArray[k].discharge.on.cnt + ":" + */
					/* BatterySummaryLevelArray[k].discharge.on.avg + "]"; */
									/* } */
								}
							}
						}
					}
					regnflag = false;
				}
			}
		}
	}

	/*
	private void debug_process()
	{
		SharedPreferences shared_prefs =
			PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = shared_prefs.edit();
		/ * editor.putString("debug_process", debug_process_line); * /
		editor.commit();
	}
	*/
}

class ComputeTime
{
	public static final int COMPUTETIMECHARSIZE = 2;
	int levelpower;		/* One byte value */
	int levelcurr;		/* One byte value */
	int levelmins;		/* Two byte value */
	/* private static Context mContext = null; */
	/* private String debug_time_line; */
	BatterySummaryLevelData summaryleveldata;

	public ComputeTime(Context context,
		BatterySummaryLevelData vsummaryleveldata)
	{
		/* mContext = context; */
		summaryleveldata = vsummaryleveldata;
		initialize();
	}

	public void initialize()
	{
		levelpower = BatteryWidgetSmartFns.BATTERY_UNPLUGGED;
		levelcurr = 0;
		levelmins = 0;
	}

	public void read(char readarray[])
	{
		levelcurr = readarray[0];				/* Two bytes */
		levelmins = readarray[1];				/* Two bytes */
	}

	public void write(char writearray[])
	{
		writearray[0] = (char) levelcurr;
		writearray[1] = (char) levelmins;
	}

	public void ProcessEntry(int currday, int currhour, int currminute,
		int pctlevel, int status, int health, int power, int runstate)
	{
		levelpower = power;
		/* TODO - one minute change not enough */
		if	(levelcurr == pctlevel)
		{
			levelmins++;
		}
		else
		{
			levelcurr = pctlevel;
			levelmins = 0;
		}
	}

	/* This algorithm cannot fill any gaps because some gaps in level */
	/* data are zero because the phone never records readings at that */
	/* percent level */
	public int BatteryTimeMins(int discharge_level, int charge_level)
	{
		int i;
		int time_sum;
		int time_set;
		boolean set_flag;
		int currindx;
		int strtindx;
		int end_indx;
		/* debug_time_line = ""; */
		currindx = summaryleveldata.level(levelcurr);
		time_sum = 0;
		set_flag = false;
		if	(levelpower == BatteryWidgetSmartFns.BATTERY_UNPLUGGED)
		{
			strtindx = currindx;
			end_indx = summaryleveldata.level(discharge_level);
			if	(end_indx > currindx)
			{
				end_indx = currindx;
			}
			/* debug_time_line += "CHG" + strtindx + "->" + end_indx + */
				/* "[" + levelmins + "]"; */
			/* Do not include the discharge_level, since this is the endpoint */
			for	(i = strtindx; i > end_indx; i--)
			{
				time_set = summaryleveldata.LevelTime(levelpower, i);
				/* debug_time_line += ":" + time_set; */
				if	(time_set > 0 && set_flag == false)
				{
					set_flag = true;
					if	(time_set >= levelmins)
					{
						time_set -= levelmins;
					}
					else
					{
						time_set = 0;
					}
				}
				time_sum += time_set;
			}
		}
		else
		{
			strtindx = currindx;
			end_indx = summaryleveldata.level(charge_level);
			if	(end_indx < currindx)
			{
				end_indx = currindx;
			}
			/* debug_time_line += "CHG" + strtindx + "->" + end_indx + */
				/* "[" + levelmins + "]"; */
			/* Do not include the charge_level, since this is the endpoint */
			for	(i = strtindx; i < end_indx; i++)
			{
				time_set = summaryleveldata.LevelTime(levelpower, i);
				/* debug_time_line += ":" + time_set; */
				if	(time_set > 0 && set_flag == false)
				{
					set_flag = true;
					if	(time_set >= levelmins)
					{
						time_set -= levelmins;
					}
					else
					{
						time_set = 0;
					}
				}
				time_sum += time_set;
			}
		}
		/* debug_time_line += "=" + time_sum; */
		/* debug_time(); */
		return(time_sum);
	}

	public int BatteryPower()
	{
		return(levelpower);
	}

	/*
	private void debug_time()
	{
		SharedPreferences shared_prefs =
			PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = shared_prefs.edit();
		/ * editor.putString("debug_time", debug_time_line); * /
		editor.commit();
	}
	*/
}

class ComputeTimeData
{
	ComputeTime computetime;
	private static int datasize;
	private static char[] databuffer;
	private static char[] dataentry;

	public ComputeTimeData(Context context,
		BatterySummaryLevelData vsummaryleveldata)
	{
		datasize = ComputeTime.COMPUTETIMECHARSIZE;
		databuffer = new char[datasize];
		dataentry = new char[datasize];
		computetime = new ComputeTime(context, vsummaryleveldata);
		computetime.initialize();
	}

	public void initialize()
	{
		computetime.initialize();
	}

	public void read(InputStreamReader rdfile) throws IOException
	{
		rdfile.read(databuffer, 0, datasize);
		System.arraycopy(databuffer, 0, dataentry, 0,
			ComputeTime.COMPUTETIMECHARSIZE);
		computetime.read(dataentry);
	}

	public void write(OutputStreamWriter wrfile) throws IOException
	{
		computetime.write(dataentry);
		System.arraycopy(dataentry, 0, databuffer, 0,
			ComputeTime.COMPUTETIMECHARSIZE);
		wrfile.write(databuffer, 0, datasize);
	}

	public void ProcessEntry(int currday, int currhour, int currminute,
		int pctlevel, int status, int health, int power, int runstate)
	{

		computetime.ProcessEntry(currday, currhour, currminute,
			pctlevel, status, health, power, runstate);
	}

	public int BatteryTimeMins(int discharge_level, int charge_level)
	{
		return(computetime.BatteryTimeMins(discharge_level, charge_level));
	}

	public int BatteryPower()
	{
		return(computetime.BatteryPower());
	}
}

class BatteryFileData
{
	public static final int BATTERYFILETIMECHARSIZE = 1;
	BatteryMinuteData minutedata;
	BatterySummaryLevelData summaryleveldata;
	ComputeTimeData computetimedata;
	private static int datasize;
	private static char[] databuffer;

	public BatteryFileData(Context context)
	{
		minutedata = new BatteryMinuteData();
		summaryleveldata = new BatterySummaryLevelData(context);
		computetimedata = new ComputeTimeData(context, summaryleveldata);
		datasize = BATTERYFILETIMECHARSIZE;
		databuffer = new char[datasize];
	}

	public void initialize()
	{
		minutedata.initialize();
		summaryleveldata.initialize();
		computetimedata.initialize();
	}

	public void read(InputStreamReader rdfile) throws IOException
	{
		rdfile.read(databuffer, 0, datasize);
		minutedata.read(rdfile);
		summaryleveldata.read(rdfile);
		computetimedata.read(rdfile);
	}

	public void write(OutputStreamWriter wrfile) throws IOException
	{
		databuffer[0] = BatteryWidgetSmartFns.VERSION_NUMBER;
		wrfile.write(databuffer, 0, datasize);
		minutedata.write(wrfile);
		summaryleveldata.write(wrfile);
		computetimedata.write(wrfile);
	}

	public void BatteryMinuteSetDay(int v_day)
	{
		minutedata.BatteryMinuteSetDay(v_day);
	}

	public int BatteryMinuteGetDay()
	{
		return(minutedata.BatteryMinuteGetDay());
	}

	/* shift() sets BatteryMinuteGetDay() to new day */
	/* Code accounts for new year */
	public void ProcessEntry(int currday, int currhour, int currminute,
		int pctlevel, int status, int health, int power, int runstate)
	{
		if	(currday == minutedata.BatteryMinuteGetDay())
		{
			/* same day - do nothing */
		}
		else
		if	(currday == (minutedata.BatteryMinuteGetDay() + 1) ||
			((currday == 0 || currday == 1) &&
			(minutedata.BatteryMinuteGetDay() >= 364)))
		{
			/* next day */
			minutedata.smooth();
			summaryleveldata.process(minutedata);
			minutedata.shift(currday);
		}
		else
		{
			/* more than one day */
			minutedata.smooth();
			summaryleveldata.process(minutedata);
			minutedata.shift(currday);
			minutedata.smooth();
			summaryleveldata.process(minutedata);
			minutedata.shift(currday);
		}
		minutedata.ProcessEntry(currday, currhour, currminute, pctlevel, status,
			health, power, runstate);
		computetimedata.ProcessEntry(currday, currhour, currminute, pctlevel,
			status, health, power, runstate);
	}

	public int BatteryTimeMins(int discharge_level, int charge_level)
	{
		return(computetimedata.BatteryTimeMins(discharge_level, charge_level));
	}

	public int BatteryPower()
	{
		return(computetimedata.BatteryPower());
	}
}

class BatteryFile
{
	private static final String BATTERYFILE = "SmartBattery.txt";
	static Context mContext = null;
	BatteryFileData filedata;

	public BatteryFile(Context context)
	{
		mContext = context;
		filedata = new BatteryFileData(context);
	}

	public void LoadFile()
	{
		InputStream fileinput;
		try
		{
			fileinput = mContext.openFileInput(BATTERYFILE);
			InputStreamReader rdfile = new InputStreamReader(fileinput);
			try
			{
				filedata.read(rdfile);
				fileinput.close();
			} catch (IOException e)
			{
				Log.e(BatteryWidgetSmartFns.LOG_TAG, "File read failed " + e);
				e.printStackTrace();
			}
		} catch (FileNotFoundException e)
		{
			Log.e(BatteryWidgetSmartFns.LOG_TAG, "File input open failed " + e);
			e.printStackTrace();
		}
	}

	public void SaveFile()
	{
		OutputStream fileoutput;
		try {
			fileoutput = mContext.openFileOutput(BATTERYFILE, 0);
			OutputStreamWriter wrfile = new OutputStreamWriter(fileoutput);
			try
			{
				filedata.write(wrfile);
				wrfile.flush();
				wrfile.close();
			} catch (IOException e)
			{
				Log.e(BatteryWidgetSmartFns.LOG_TAG, "File write failed " + e);
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			Log.e(BatteryWidgetSmartFns.LOG_TAG, "File output open failed " + e);
			e.printStackTrace();
		}
	}

	public void BatteryMinuteSetDay(int v_day)
	{
		filedata.BatteryMinuteSetDay(v_day);
	}

	public int BatteryMinuteGetDay()
	{
		return(filedata.BatteryMinuteGetDay());
	}

	public int BatteryTimeMins(int discharge_level, int charge_level)
	{
		return(filedata.BatteryTimeMins(discharge_level, charge_level));
	}

	public int BatteryPower()
	{
		return(filedata.BatteryPower());
	}

	public void ProcessEntry(int currday, int currhour, int currminute,
		int pctlevel, int status, int health, int power, int runstate)
	{
		filedata.ProcessEntry(currday, currhour, currminute,
			pctlevel, status, health, power, runstate);
	}
}
