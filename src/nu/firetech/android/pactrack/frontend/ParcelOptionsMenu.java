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
import nu.firetech.android.pactrack.backend.Preferences;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

public class ParcelOptionsMenu {
	private static final int AUTO_ID = Menu.FIRST;

	private long mRowId;
	private int mPosition;
	private ParcelDbAdapter mDbAdapter;
	private UpdateableView mViewToUpdate;
	
	public ParcelOptionsMenu(Context ctx, Menu parentMenu, long rowId, int position, ParcelDbAdapter dbAdapter, UpdateableView viewToUpdate) {
		mRowId = rowId;
		mPosition = position;
		mDbAdapter = dbAdapter;
		mViewToUpdate = viewToUpdate;

		Preferences pref = Preferences.getPreferences(ctx);

		Listener l = new Listener();
		boolean enabled = pref.getCheckInterval() > 0;
		
		parentMenu.add(0, AUTO_ID, 0, R.string.menu_auto_include)
				.setEnabled(enabled)
				.setCheckable(true)
				.setChecked(enabled && dbAdapter.getAutoUpdate(rowId))
				.setOnMenuItemClickListener(l);
	}
	
	public class Listener implements OnMenuItemClickListener {
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			switch(item.getItemId()) {
			case AUTO_ID:
				boolean autoUpdate = !item.isChecked();
				mDbAdapter.setAutoUpdate(mRowId, autoUpdate);
				item.setChecked(autoUpdate);
				if (mViewToUpdate != null) {
					mViewToUpdate.updateAutoUpdateView(mPosition, autoUpdate);
				}
				return true;
			}
			return false;
		}
	}
	
	public interface UpdateableView {
		public void updateAutoUpdateView(int position, boolean value);
	}
}
