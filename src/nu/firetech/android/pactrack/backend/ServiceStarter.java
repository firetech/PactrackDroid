/*
 * Copyright (C) 2011 Joakim Andersson
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

public class ServiceStarter extends BroadcastReceiver {
	private static final String TAG = "<PactrackDroid> ServiceStarter";
	
	private static long sCurrentInterval = -1;

	@Override
	public void onReceive(Context ctx, Intent intent) {
		// The only received intents here are BOOT_COMPLETED or BACKGROUND_DATA_SETTING_CHANGED
		ParcelDbAdapter dbAdapter = new ParcelDbAdapter(ctx);
		dbAdapter.open();
		startService(ctx, dbAdapter);
		dbAdapter.close();
	}
	
	public static void startService(Context ctx, ParcelDbAdapter dbAdapter) {
		Preferences pref = Preferences.getPreferences(ctx);
		long interval = pref.getCheckInterval();
		
		startService(ctx, dbAdapter, interval);
	}
	
	@SuppressWarnings("deprecation")
	public static void startService(Context ctx, ParcelDbAdapter dbAdapter, long interval) {
		PendingIntent pi = PendingIntent.getService(ctx, 0, new Intent(ctx, ParcelService.class), 0);
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		
		ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if (dbAdapter != null && dbAdapter.getNumAutoParcels() < 1) {
			interval = 0;
		}

		if ((cm.getBackgroundDataSetting() ? interval : 0) > 0) {
			Log.d(TAG, "Scheduling service to be run every " + interval + " milliseconds (inexact)");
			am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (interval / 2), interval, pi);
		} else {
			Log.d(TAG, "Removing any existing service alarms");
			am.cancel(pi);
		}
		
		sCurrentInterval = interval;
	}
	
	public static long getCurrentInterval() {
		return sCurrentInterval;
	}
}
