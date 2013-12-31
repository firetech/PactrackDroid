/*
 * Copyright (C) 2013 Joakim Andersson
 * Copyright (C) 2013 blunden
 * 
 * This file is part of PactrackDroid, an Android application to keep
 * track of parcels sent with the Swedish mail service (Posten).
 * 
 * PactrackDroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * PactrackDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package nu.firetech.android.pactrack.backend;

import nu.firetech.android.pactrack.R;
import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;

public class Preferences {
	private static Preferences instance;
	
	private SharedPreferences mPrefs;
	private Resources mRes;
	
	private Preferences(SharedPreferences prefs, Resources res)
	{
		mPrefs = prefs;
		mRes = res;
	}
	
	public static synchronized Preferences getPreferences(Context ctx)
	{
		if(instance == null)
		{
			instance = new Preferences(PreferenceManager.getDefaultSharedPreferences(ctx), ctx.getResources());
		}
		
		return instance;
	}
	
	public long getCheckInterval() {
		if (!mPrefs.getBoolean(mRes.getString(R.string.key_auto_updates), mRes.getBoolean(R.bool.auto_updates_default))) {
			return 0;
		}
		
		int interval = 0;
		try {
			interval = Integer.parseInt(mPrefs.getString(mRes.getString(R.string.key_check_interval),
					mRes.getString(R.string.check_interval_default)));
		} catch (Exception e) {}
		
		switch(interval) {
		case 15:
			return AlarmManager.INTERVAL_FIFTEEN_MINUTES;
		case 30:
			return AlarmManager.INTERVAL_HALF_HOUR;
		case 60:
			return AlarmManager.INTERVAL_HOUR;
		case 720:
			return AlarmManager.INTERVAL_HALF_DAY;
		case 1440:
			return AlarmManager.INTERVAL_DAY;
		default:
			return 0;
		}
	}
	
	public boolean getNotificationEnabled() {
		return mPrefs.getBoolean(mRes.getString(R.string.key_notify_on), mRes.getBoolean(R.bool.notify_on_default));
	}
	
	public boolean getNotificationVibrate() {
		return mPrefs.getBoolean(mRes.getString(R.string.key_notify_vibrate), mRes.getBoolean(R.bool.notify_vibrate_default));
	}
	
	public boolean getNotificationLight() {
		return mPrefs.getBoolean(mRes.getString(R.string.key_notify_light), mRes.getBoolean(R.bool.notify_light_default));
	}
	
	public int getNotificationColor() {
		return Integer.parseInt(mPrefs.getString(mRes.getString(R.string.key_notify_light_color),
				mRes.getString(R.string.notify_light_color_default)), 16) + 0xff000000;
	}
	
	public int getNotificationOntime() {
		return Integer.parseInt(mPrefs.getString(mRes.getString(R.string.key_notify_light_ontime),
				mRes.getString(R.string.notify_light_ontime_default)));
	}
	
	public int getNotificationOfftime() {
		return Integer.parseInt(mPrefs.getString(mRes.getString(R.string.key_notify_light_offtime),
				mRes.getString(R.string.notify_light_offtime_default)));
	}
	
	public Uri getNotificationSound() {
		String uri = mPrefs.getString(mRes.getString(R.string.key_notify_sound), mRes.getString(R.string.notify_sound_default));
		return (uri == null || uri.length() == 0 ? null : Uri.parse(uri));
	}
}
