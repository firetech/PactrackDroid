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

import java.util.ArrayList;

import nu.firetech.android.pactrack.R;
import nu.firetech.android.pactrack.common.ContextListener;
import nu.firetech.android.pactrack.common.RefreshContext;
import nu.firetech.android.pactrack.frontend.MainWindow;
import nu.firetech.android.pactrack.frontend.ParcelView;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class ParcelUpdater extends BroadcastReceiver implements Runnable, ContextListener {
	private static final String TAG = "<PactrackDroid> ParcelUpdater";

	public static void update(final RefreshContext ctx, Cursor parcel, ParcelDbAdapter dbAdapter) {
		ArrayList<Bundle> workParcels = new ArrayList<Bundle>();
		workParcels.add(cursorToBundle(parcel));

		new Thread(new ParcelUpdater(workParcels, ctx)).start();
	}

	public static void updateAll(boolean autoOnly, final RefreshContext ctx, ParcelDbAdapter dbAdapter) {
		final Cursor parcel = dbAdapter.fetchAllParcels(autoOnly);
		if (parcel == null) {
			MainWindow.dbErrorDialog((Context)ctx);
			return;
		}

		ArrayList<Bundle> workParcels = new ArrayList<Bundle>();
		parcel.moveToFirst();
		while (!parcel.isAfterLast()) {
			workParcels.add(cursorToBundle(parcel));
			parcel.moveToNext();
		}

		parcel.close();

		if (workParcels.isEmpty()) {
			ctx.refreshDone();
			return;
		}

		new Thread(new ParcelUpdater(workParcels, ctx)).start();
	}

	private static Bundle cursorToBundle(Cursor parcel) {
		Bundle parcelBundle = new Bundle();

		parcelBundle.putLong(ParcelDbAdapter.KEY_ROWID,
				parcel.getLong(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_ROWID)));
		parcelBundle.putString(ParcelDbAdapter.KEY_PARCEL,
				parcel.getString(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_PARCEL)));
		parcelBundle.putString(ParcelDbAdapter.KEY_NAME,
				parcel.getString(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_NAME)));

		return parcelBundle;
	}

	////////////////////////////////////////////////////////////////////////////////

	private ConnectivityManager mConnectivityManager;
	private static boolean sWaitingForDataConnection = false;
	private static final Object sLock = new Object();

	@Override
	public void onReceive(final Context ctx, Intent intent) {
		if (!isDataConnected()) {
			return;
		}
		
		Log.d(TAG, "We are back online!");
		synchronized (sLock) {
			sLock.notifyAll();
			ctx.unregisterReceiver(ParcelUpdater.this);
			sWaitingForDataConnection = false;
		}
	}
	
	private void registerConnectionListener(Context ctx) {
		synchronized (sLock) {
	        IntentFilter intentFilter = new IntentFilter();
	        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
	        ctx.registerReceiver(this, intentFilter);
			
			sWaitingForDataConnection = true;
		}
	}

	////////////////////////////////////////////////////////////////////////////////

	private ArrayList<Bundle> mWorkParcels = null;
	private ParcelDbAdapter mDbAdapter = null;
	private RefreshContext mCtx = null;

	private ParcelUpdater(ArrayList<Bundle> workParcels, RefreshContext ctx) {
		mWorkParcels = workParcels;
		mCtx = ctx;

		mConnectivityManager = (ConnectivityManager)((Context)ctx).getSystemService(Context.CONNECTIVITY_SERVICE);

		ctx.startRefreshProgress(workParcels.size(), this);
	}

	@Override
	public void onContextDestroy(Context oldContext) {}
	
	@Override
	public void onContextChange(Context newContext) {
		mCtx = (RefreshContext)newContext;
	}

	@Override
	public void run() {
		synchronized (sLock) {
			if(sWaitingForDataConnection) {
				Log.i(TAG, "Another update is waiting for data connection. Skipping");
				return;
			}
		}

		//wait for a data connection
		while(!isDataConnected()) {
			registerConnectionListener((Context)mCtx);
			synchronized (sLock) {
				Log.d(TAG, "No data connection, waiting for a data connection");
				try {
					sLock.wait();
				} catch (InterruptedException e) {
					Log.e(TAG, "Error while waiting for connection", e);
				}
			}
		}
		
		mDbAdapter = new ParcelDbAdapter((Context)mCtx);
		mDbAdapter.open();
		
		NotificationManager notMgr = (NotificationManager)((Context)mCtx).getSystemService(Context.NOTIFICATION_SERVICE);

		for(int i = 0; i < mWorkParcels.size(); i++) {
			updateParcel(mWorkParcels.get(i), mDbAdapter, notMgr);
			mCtx.getProgressHandler().sendEmptyMessage(i + 1);
		}

		new Handler(((Context)mCtx).getMainLooper()) {
			@Override
			public void handleMessage(Message m) {
				mCtx.refreshDone();
			}
		}.sendEmptyMessage(0);
		
		mDbAdapter.close();
	}

	private boolean isDataConnected() {
		NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
		return (info != null && info.isConnected());
	}

	private void updateParcel(Bundle parcel, ParcelDbAdapter dbAdapter, NotificationManager notMgr) {
		Long rowId = parcel.getLong(ParcelDbAdapter.KEY_ROWID);
		String parcelId = parcel.getString(ParcelDbAdapter.KEY_PARCEL);

		Parcel parcelData = ParcelXMLParser.fetch(parcelId);

		mDbAdapter.updateParcelData(rowId, parcelData);
		if (parcelData.getParcel() != null) {
			parcelId = parcelData.getParcel();
		}
		
		Context realCtx = (Context)mCtx;

		int newEvents = 0;
		StringBuilder eventList = new StringBuilder();
		for (ParcelEvent eventData : parcelData.getEvents()) { //loop through events
			if (mDbAdapter.addEvent(rowId, eventData)) { //if event was new
				if (newEvents > 0) {
					eventList.append("\n");
				}
				eventList.append(eventData.toString());
				newEvents++;
			}
		}

		if (newEvents > 0 && !mCtx.showsNews()) {
			Preferences prefs = Preferences.getPreferences(realCtx);

			if (prefs.getNotificationEnabled()) {
				String parcelName = parcel.getString(ParcelDbAdapter.KEY_NAME);
				if(parcelName == null) {
					parcelName = realCtx.getString(R.string.generic_parcel_name, parcelId);
				}

				Intent intent = new Intent(realCtx, ParcelView.class).putExtra(ParcelDbAdapter.KEY_ROWID, rowId);
				PendingIntent contentIntent = PendingIntent.getActivity(realCtx, rowId.hashCode(), intent, 0);
				
				int stringId = (newEvents > 1 ? R.string.notification_ticker : R.string.notification_ticker_one);
				NotificationCompat.Builder n = new NotificationCompat.Builder(realCtx)
						.setSmallIcon(R.drawable.notification)
						.setTicker(realCtx.getString(stringId, parcelName))
						.setWhen(System.currentTimeMillis())
						.setContentTitle(parcelName)
						.setContentText(newEvents == 1 ?
								eventList.toString() : realCtx.getString(R.string.notification_message, newEvents))
						.setContentIntent(contentIntent)
						.setSound(prefs.getNotificationSound());
				
				if (newEvents > 1) {
					n.setStyle(new NotificationCompat.BigTextStyle().bigText(eventList.toString()));
				}
				
				if (prefs.getNotificationLight()) {
					n.setLights(prefs.getNotificationColor(), 
							prefs.getNotificationOntime(), 
							prefs.getNotificationOfftime());
				}
				if (prefs.getNotificationVibrate()) {
					n.setDefaults(Notification.DEFAULT_VIBRATE);
				}

				notMgr.notify(rowId.hashCode(), n.build());
			}
		}
	}
}
