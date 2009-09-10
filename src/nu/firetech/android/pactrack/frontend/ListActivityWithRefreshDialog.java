/*
 * Copyright (C) 2009 Joakim Andersson
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

package nu.firetech.android.pactrack.frontend;

import java.util.ArrayList;

import nu.firetech.android.pactrack.R;
import nu.firetech.android.pactrack.common.RefreshContext;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ListActivityWithRefreshDialog extends ListActivity implements RefreshContext {
	private static boolean sDialogShowing;
	private static int sDialogMax;
	private static int sDialogValue;
	static ArrayList<RefreshContext.Listener> sListeners = new ArrayList<RefreshContext.Listener>(); 
	
	protected ProgressDialog mPd;
	private ProgressHandler mHandler = new ProgressHandler();

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setUpRefreshDialog(sDialogMax);
	    if (sDialogShowing) {
	    	showRefreshDialog();
	    	if (!sListeners.isEmpty()) {
	    		for (RefreshContext.Listener l : sListeners) {
	    			try {
	    			    l.onContextChange(this);
	    			} catch (Exception e) {} // Ignore errors, null pointers for example.
	    		}
	    	}
	    }
	}
	
	@Override
    protected void onPause() {
		super.onPause();
    	dismissRefreshDialog();
	}

////////////////////////////////////////////////////////////////////////////////

	@Override
	public Handler getRefreshHandler() {
		return mHandler;
	}
	
	void setUpRefreshDialog(int maxValue) {
		sDialogMax = maxValue;
		if (mPd == null) {
    		mPd = new ProgressDialog(this);
            mPd.setTitle(R.string.refreshing);
		}
		
        if (sDialogMax > 1) {
        	mPd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        	mPd.setMax(sDialogMax);
        	mPd.setMessage(null);
        } else {
        	mPd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    	    mPd.setMessage(getString(R.string.refreshing));
        }
	}
	
	@Override
	public void startRefreshDialog(int maxValue, RefreshContext.Listener listener){
		setUpRefreshDialog(maxValue);
		sListeners.add(listener);
		showRefreshDialog();
	}
		
	void showRefreshDialog() {
		sDialogShowing = true;
		if (mPd != null && !mPd.isShowing()) {
		    mPd.show();
		    mPd.setProgress(sDialogValue);
		}
	}
	
	private void dismissRefreshDialog() {
		if (mPd != null && mPd.isShowing()) {
		    mPd.dismiss();
		}
	}

////////////////////////////////////////////////////////////////////////////////

	private class ProgressHandler extends Handler {
		@Override
        public void handleMessage(Message m) {
			if (m.what == sDialogMax) {
			    sDialogShowing = false;
			    sListeners.clear();
				dismissRefreshDialog();
				refreshDone();
			} else {
				if (mPd != null && mPd.isShowing()) {
				    mPd.setProgress(m.what);
				}
		    }
        }
	};

	// To be overridden by subclasses if needed.
	public void refreshDone() {}
}
