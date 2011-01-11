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

/*
 * Copyright (c) 2010 Sony Ericsson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package nu.firetech.android.pactrack.backend;

import nu.firetech.android.pactrack.R;
import nu.firetech.android.pactrack.frontend.ParcelView;

import com.sonyericsson.extras.liveview.plugins.AbstractPluginService;
import com.sonyericsson.extras.liveview.plugins.PluginConstants;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class LiveViewService extends AbstractPluginService {
	/**
	 * Plugin is just sending notifications.
	 */
	protected boolean isSandboxPlugin() {
	    return false;
	}

	/**
     * When a user presses the "open in phone" button on the LiveView device, this method is called.
     * 
     * Open the ParcelView for the specified parcel.
     */
	protected void openInPhone(String rowId) {
		Log.d(PluginConstants.LOG_TAG, "openInPhone: " + rowId);
		Intent i = new Intent(this, ParcelView.class)
			.putExtra(ParcelDbAdapter.KEY_ROWID, Long.parseLong(rowId))
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}
	

	@Override
	public void onStart(Intent intent, int startId) {
		if (intent.getAction().equals(getString(R.string.intent_announce))) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				sendAnnounce(
						extras.getString(ParcelDbAdapter.KEY_PARCEL),
						extras.getString(ParcelDbAdapter.KEY_STATUS),
						extras.getLong(ParcelDbAdapter.KEY_ROWID)
				);
			}
		} else {
			super.onStart(intent, startId);
		}
	}
	
	/**
	 * Send announcement to LiveView device.
	 */
	private void sendAnnounce(String parcelId, String body, long rowId) {
		try {
			if((mLiveViewAdapter != null) && mSharedPreferences.getBoolean(PluginConstants.PREFERENCES_PLUGIN_ENABLED, false)) {
			    mLiveViewAdapter.sendAnnounce(mPluginId, mMenuIcon, parcelId, body, System.currentTimeMillis(), String.valueOf(rowId));
				Log.d(PluginConstants.LOG_TAG, "Announce sent to LiveView");
			} else {
				Log.d(PluginConstants.LOG_TAG, "LiveView not reachable");
			}
		} catch(Exception e) {
			Log.e(PluginConstants.LOG_TAG, "Failed to send announce", e);
		}
	}

	// The following methods must be implemented, but are not used.
	protected void startPlugin() {}
	protected void stopPlugin() {}
	protected void startWork() {}
	protected void stopWork() {}
	protected void onUnregistered() {}
	protected void onSharedPreferenceChangedExtended(SharedPreferences prefs, String key) {}
	protected void onServiceConnectedExtended(ComponentName className, IBinder service) {}
	protected void onServiceDisconnectedExtended(ComponentName className) {}
    protected void screenMode(int mode) {}
	protected void button(String buttonType, boolean doublepress, boolean longpress) {}
	protected void displayCaps(int displayWidthPx, int displayHeigthPx) {}			
}