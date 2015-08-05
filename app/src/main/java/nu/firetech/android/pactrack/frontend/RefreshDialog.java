/*
 * Copyright (C) 2015 Joakim Tufvegren
 * Copyright (C) 2014 blunden
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
import nu.firetech.android.pactrack.backend.ParcelDbAdapter;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class RefreshDialog extends DialogFragment {
	private ProgressHandler mHandler;

	public static Handler show(Activity context, int maxValue) {
		RefreshDialog d = new RefreshDialog();
		d.initValues(maxValue);
		d.show(context.getFragmentManager(), RefreshDialog.class.getName());
		return d.mHandler;
	}

	public void initValues(int maxValue) {
		mHandler = new ProgressHandler(this, maxValue);
		setRetainInstance(true);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (mHandler == null) {
			throw new IllegalStateException("RefreshDialog.initValues() has not been called.");
		}

		ProgressDialog dialog = new ProgressDialog(getActivity());
		dialog.setMax(mHandler.getMaxValue());
		dialog.setTitle(R.string.refreshing);
		dialog.setMessage(getString(R.string.refreshing));
		if (dialog.getMax() > 1) {
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		} else {
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		}

		return dialog;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		// If we set the progress before the dialog is showing, it won't apply.
		((ProgressDialog)getDialog()).setProgress(mHandler.getValue());
	}
	
	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance())
			getDialog().setDismissMessage(null);
		super.onDestroyView();
	}

	private static class ProgressHandler extends Handler {
		RefreshDialog mParent;
		int mMaxValue;
		int mValue;

		public ProgressHandler(RefreshDialog parent, int maxValue) {
			mParent = parent;
			mMaxValue = maxValue;
			mValue = 0;
		}

		public int getValue() {
			return mValue;
		}

		public int getMaxValue() {
			return mMaxValue;
		}

		@Override
		public void handleMessage(Message m) {
			mValue = m.what;
			if (m.what == mMaxValue) {
				mParent.setRetainInstance(false);
				mParent.dismiss();
			} else if (mParent.getDialog() != null) {
				ProgressDialog dialog = (ProgressDialog)mParent.getDialog();
				dialog.setProgress(m.what);
				if (m.obj instanceof Bundle) {
					Bundle bundle = (Bundle)m.obj;
					String parcel = bundle.getString(ParcelDbAdapter.KEY_NAME);
					if (parcel == null) {
						parcel = bundle.getString(ParcelDbAdapter.KEY_PARCEL);
					}
					dialog.setMessage(parcel);
				}
			}
		}
	};
}
