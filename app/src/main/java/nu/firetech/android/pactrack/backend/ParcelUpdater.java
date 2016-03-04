/*
 * Copyright (C) 2016 Joakim Tufvegren
 * Copyright (C) 2016 blunden
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import nu.firetech.android.pactrack.R;
import nu.firetech.android.pactrack.common.RefreshContext;
import nu.firetech.android.pactrack.frontend.MainActivity;
import nu.firetech.android.pactrack.frontend.UICommon;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class ParcelUpdater extends BroadcastReceiver implements Runnable {
	private static final String TAG = "<PactrackDroid> ParcelUpdater";

	public static void update(final RefreshContext ctx, Cursor parcel) {
		ArrayList<Bundle> workParcels = new ArrayList<>();
		workParcels.add(cursorToBundle(parcel));

		new Thread(new ParcelUpdater(workParcels, ctx)).start();
	}

	public static void updateAll(boolean autoOnly, final RefreshContext ctx, ParcelDbAdapter dbAdapter) {
		final Cursor parcel = dbAdapter.fetchAllParcels(autoOnly);
		if (parcel == null) {
			UICommon.dbErrorDialog((Context)ctx);
			return;
		}

		ArrayList<Bundle> workParcels = new ArrayList<>();
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
	private Context mAndroidCtx = null;
	private Handler mHandler = null;

	private ParcelUpdater(ArrayList<Bundle> workParcels, RefreshContext ctx) {
		mWorkParcels = workParcels;
		mCtx = ctx;
		
		if (ctx instanceof Context) {
			mAndroidCtx = (Context)ctx;
		} else if (ctx instanceof Fragment) {
			mAndroidCtx = ((Fragment)ctx).getActivity();
		} else {
			throw new IllegalArgumentException("Unknown context type!");
		}
		ParcelJsonParser.loadApiKey(mAndroidCtx);

		mConnectivityManager = (ConnectivityManager)mAndroidCtx.getSystemService(Context.CONNECTIVITY_SERVICE);

		mHandler = ctx.startRefreshProgress(workParcels.size());
		if (mHandler != null) {
			mHandler.sendMessage(Message.obtain(mHandler, 0, workParcels.get(0)));
		}
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
			registerConnectionListener(mAndroidCtx);
			synchronized (sLock) {
				Log.d(TAG, "No data connection, waiting for a data connection");
				try {
					sLock.wait();
				} catch (InterruptedException e) {
					Log.e(TAG, "Error while waiting for connection", e);
				}
			}
		}
		
		mDbAdapter = new ParcelDbAdapter(mAndroidCtx);
		mDbAdapter.open();
		
		NotificationManager notMgr = (NotificationManager)mAndroidCtx.getSystemService(Context.NOTIFICATION_SERVICE);

		for(int i = 0; i < mWorkParcels.size(); i++) {
			Bundle parcel = mWorkParcels.get(i);
			if (mHandler != null) {
				mHandler.sendMessage(Message.obtain(mHandler, i, parcel));
			}
			updateParcel(parcel, mDbAdapter, notMgr);
		}
		if (mHandler != null) {
			mHandler.sendEmptyMessage(mWorkParcels.size());
		}

		new RefreshDoneHandler(new WeakReference<>(this),
				mAndroidCtx.getMainLooper()).sendEmptyMessage(0);
		
		mDbAdapter.close();
	}

	private static class RefreshDoneHandler extends Handler {
		WeakReference<ParcelUpdater> mParent;

		RefreshDoneHandler(WeakReference<ParcelUpdater> parent, Looper looper) {
			super(looper);
			mParent = parent;
		}

		@Override
		public void handleMessage(Message m) {
			mParent.get().mCtx.refreshDone();
		}
	}
	
	public void setContext(RefreshContext newContext) {
		mCtx = newContext;
	}

	private boolean isDataConnected() {
		NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
		return (info != null && info.isConnected());
	}

	private void updateParcel(Bundle parcel, @SuppressWarnings("unused") ParcelDbAdapter dbAdapter, NotificationManager notMgr) {
		Long rowId = parcel.getLong(ParcelDbAdapter.KEY_ROWID);
		String parcelId = parcel.getString(ParcelDbAdapter.KEY_PARCEL);

		Parcel parcelData = ParcelJsonParser.fetch(parcelId);

		mDbAdapter.updateParcelData(rowId, parcelData);
		if (parcelData.getParcel() != null) {
			parcelId = parcelData.getParcel();
		}

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
			Preferences prefs = Preferences.getPreferences(mAndroidCtx);

			//TODO Join existing notifications in some clever way...
			if (prefs.getNotificationEnabled()) {
				String parcelName = parcel.getString(ParcelDbAdapter.KEY_NAME);
				if(parcelName == null) {
					parcelName = mAndroidCtx.getString(R.string.generic_parcel_name, parcelId);
				}

				Intent intent = new Intent(mAndroidCtx, MainActivity.class).putExtra(ParcelDbAdapter.KEY_ROWID, rowId);
				PendingIntent contentIntent = PendingIntent.getActivity(mAndroidCtx, rowId.hashCode(), intent, 0);
				
				int stringId = (newEvents > 1 ? R.string.notification_ticker : R.string.notification_ticker_one);
				NotificationCompat.Builder n = new NotificationCompat.Builder(mAndroidCtx)
						.setSmallIcon(R.drawable.notification)
						.setTicker(mAndroidCtx.getString(stringId, parcelName))
						.setWhen(System.currentTimeMillis())
						.setContentTitle(parcelName)
						.setContentText(newEvents == 1 ?
								eventList.toString() : mAndroidCtx.getString(R.string.notification_message, newEvents))
						.setContentIntent(contentIntent)
						.setSound(prefs.getNotificationSound())
						.extend(new NotificationCompat.WearableExtender().setBackground(
								BitmapFactory.decodeResource(mAndroidCtx.getResources(), R.drawable.wearable_background)));
				
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
