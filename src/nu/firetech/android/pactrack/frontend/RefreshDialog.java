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

package nu.firetech.android.pactrack.frontend;

import nu.firetech.android.pactrack.R;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class RefreshDialog extends ProgressDialog implements DialogAwareListActivity.Dialog {
	private static final String KEY_MAX_VALUE = "max_value";
	private static final String KEY_VALUE = "value";

	private int mMaxValue;
	private ProgressHandler mHandler;
	
	public static void show(final DialogAwareListActivity context, int maxValue) {
		RefreshDialog d = new RefreshDialog(context, 0, maxValue);
		d.show();
	}
	
	public static void show(final DialogAwareListActivity context, Bundle dialogData) {
		RefreshDialog d = new RefreshDialog(context, dialogData.getInt(KEY_VALUE), dialogData.getInt(KEY_MAX_VALUE));
		d.show();
	}

	private RefreshDialog(final DialogAwareListActivity context, int initialValue, int maxValue) {
		super(context);
		
		setProgress(initialValue);
		mMaxValue = maxValue;
		mHandler = new ProgressHandler();

		context.addDialog(this);
		setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				context.removeDialog(RefreshDialog.this);
			}
		});
		
        setTitle(R.string.refreshing);
    	setMax(mMaxValue);
        if (mMaxValue > 1) {
        	setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        	setMessage(null);
        } else {
        	setProgressStyle(ProgressDialog.STYLE_SPINNER);
    	    setMessage(context.getString(R.string.refreshing));
        }
	}

	@Override
	public Bundle getInstanceState() {
		Bundle outState = new Bundle(2);
		outState.putInt(KEY_VALUE, getProgress());
		outState.putInt(KEY_MAX_VALUE, mMaxValue);
		return outState;
	}
	
	public Handler getProgressHandler() {
		return mHandler;
	}

	@Override
	public void onContextChange(Context newContext) {}

	@Override
	public void onContextDestroy(Context oldContext) {
		dismiss();
	}
	
	private class ProgressHandler extends Handler {
		@Override
        public void handleMessage(Message m) {
			if (m.what == mMaxValue) {
				dismiss();
			} else {
				setProgress(m.what);
		    }
        }
	};
}
