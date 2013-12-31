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

import nu.firetech.android.pactrack.common.RefreshContext;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class ParcelService extends Service implements RefreshContext {
	private static final String TAG = "<PactrackDroid> ParcelService";

	private ParcelDbAdapter mDbAdapter;

	@Override
	public void onCreate() {
		mDbAdapter = new ParcelDbAdapter(this);
		mDbAdapter.open();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(TAG, "Automatic update initiated");
		ParcelUpdater.updateAll(true, this, mDbAdapter);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//For Android 2.0+, start the service and tell the system that we don't need to stay running.
		onStart(intent, startId);		
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		mDbAdapter.close();
	}

	@Override
	public Handler startRefreshProgress(int maxValue, ParcelUpdater updater) {
		Log.d(TAG, "Automatic update running");
		return null;
	}

	@Override
	public void refreshDone() {
		stopSelf();	
	}

	@Override
	public boolean showsNews() {
		return false;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
