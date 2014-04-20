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
import nu.firetech.android.pactrack.backend.ParcelDbAdapter;
import nu.firetech.android.pactrack.backend.ParcelXMLParser;
import nu.firetech.android.pactrack.common.Error;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;

public class UICommon {

	public static void dbErrorDialog(Context c) {
		new AlertDialog.Builder(c)
		.setTitle(R.string.db_error_title)
		.setMessage(R.string.db_error_message)
		.setIconAttribute(android.R.attr.alertDialogIcon)
		.setNeutralButton(R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.create()
		.show();
	}

	public static int getStatusImage(Cursor parcel, int statusColumnIndex) {
		int statusCode = parcel.getInt(statusColumnIndex);
		if (parcel.getInt(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_ERROR)) != Error.NONE) {
			return R.drawable.ic_parcel_error;
		} else if (statusCode == ParcelXMLParser.STATUS_PREINFO) {
			return R.drawable.ic_parcel_preinfo;
		} else if (statusCode == ParcelXMLParser.STATUS_COLLECTABLE) {
			return R.drawable.ic_parcel_collectable;
		} else if (statusCode == ParcelXMLParser.STATUS_DELIVERED) {
			return R.drawable.ic_parcel_delivered;
		} else {
			return R.drawable.ic_parcel_enroute; //This has multiple codes (3 and 5 are confirmed)
		}
	}

	public static void deleteParcel(final long rowId, Context c, final ParcelDbAdapter dbAdapter, final Runnable r) {
		new AlertDialog.Builder(c)
		.setTitle(R.string.remove_confirm_title)
		.setMessage(R.string.remove_confirm_message)
		.setIconAttribute(android.R.attr.alertDialogIcon)
		.setPositiveButton(R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dbAdapter.deleteParcel(rowId);
				if (r != null) {
					r.run();
				}
				dialog.dismiss();
			}
		})
		.setNegativeButton(R.string.cancel, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.create()
		.show();
	}

}
