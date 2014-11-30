/*
 * Copyright (C) 2014 Joakim Andersson
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
import nu.firetech.android.pactrack.backend.Preferences;
import nu.firetech.android.pactrack.backend.ServiceStarter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity implements
		ParcelListFragment.ParentActivity, ParcelDetailsFragment.ParentActivity,
		ParcelIdDialog.ParentActivity {
	private final static String KEY_DISPLAY_OPT = "display_options";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_window);

		// Start service if it isn't running (not entirely fool-proof, but works)
		if (ServiceStarter.getCurrentInterval() == -1) {
			ParcelDbAdapter dbAdapter = new ParcelDbAdapter(this);
			dbAdapter.open();
			ServiceStarter.startService(this, dbAdapter);
			dbAdapter.close();
		}

		// Using the single pane layout, set initial fragment.
		if (findViewById(R.id.fragment_container) != null && savedInstanceState == null) {
			ParcelListFragment firstFragment = new ParcelListFragment();
			firstFragment.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction()
			.add(R.id.fragment_container, firstFragment)
			.commit();
		}
		
		if (getIntent() != null) {
			onNewIntent(getIntent());
		}
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		Bundle extras = (intent != null ? intent.getExtras() : null);
		if (extras != null) {
			if (extras.containsKey(ParcelDbAdapter.KEY_ROWID)) {
				showParcel(extras.getLong(ParcelDbAdapter.KEY_ROWID),
						extras.getBoolean(ParcelDetailsFragment.FORCE_REFRESH));
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_DISPLAY_OPT, getSupportActionBar().getDisplayOptions());
	}


	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		int savedDisplayOpt = savedInstanceState.getInt(KEY_DISPLAY_OPT);
		if(savedDisplayOpt != 0){
			getSupportActionBar().setDisplayOptions(savedDisplayOpt);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			getSupportFragmentManager().popBackStack();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void showAndRefreshParcel(long rowId) {
		showParcel(rowId, true);
	}

	@Override
	public void showParcel(long rowId, boolean forceRefresh) {
		FragmentManager manager = getSupportFragmentManager();
		
		if (manager.findFragmentById(R.id.details_frag) == null) {
			// First load of fragment or one pane layout
			ParcelDetailsFragment newFragment = new ParcelDetailsFragment();
			Bundle args = new Bundle();
			args.putLong(ParcelDbAdapter.KEY_ROWID, rowId);
			if (forceRefresh) {
				args.putBoolean(ParcelDetailsFragment.FORCE_REFRESH, true);
			}
			newFragment.setArguments(args);

			int viewId = R.id.details_frag;
			boolean fullscreen = false;
			if (findViewById(viewId) == null) {
				viewId = R.id.fragment_container;
				fullscreen = true;
				
				// Avoid duplicated parcels in back stack
				if (manager.findFragmentById(viewId) instanceof ParcelDetailsFragment &&
						((ParcelDetailsFragment)manager.findFragmentById(viewId)).getCurrentRowId() == rowId) {
					manager.popBackStackImmediate();
				}
			}

			FragmentTransaction transaction = manager.beginTransaction();
			transaction.replace(viewId, newFragment);
			if (fullscreen) {
				transaction.addToBackStack(null);
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			}
			transaction.commit();
			
		} else {
			ParcelDetailsFragment fragment = (ParcelDetailsFragment)manager.findFragmentById(R.id.details_frag);
			fragment.switchParcel(rowId, forceRefresh);
		}

		if (findViewById(R.id.list_frag) != null) {
			ParcelListFragment list = (ParcelListFragment)manager.findFragmentById(R.id.list_frag);
			list.selectRowItem(rowId);
			if (forceRefresh) {
				list.refreshDone();
			}
		}
	}

	@Override
	public void onListResume() {
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		setTitle(R.string.app_name);
	}

	@Override
	public void onCurrentParcelRemoved() {
		FragmentManager manager = getSupportFragmentManager();

		if (manager.findFragmentById(R.id.details_frag) != null) {
			FragmentTransaction transaction = manager.beginTransaction();
			transaction.remove(manager.findFragmentById(R.id.details_frag));
			transaction.commit();
		} else if (manager.getBackStackEntryCount() > 0) {
			manager.popBackStack();
		}

		if (manager.findFragmentById(R.id.list_frag) != null) {
			ParcelListFragment list = (ParcelListFragment)getSupportFragmentManager().findFragmentById(R.id.list_frag);
			list.selectRowItem(null);
			list.refreshDone();
		}
	}
	
	@Override
	public void onAutoUpdateChanged(long rowId, boolean value) {
		FragmentManager manager = getSupportFragmentManager();
		
		if (Preferences.getPreferences(this).getCheckInterval() == 0) {
			value = true;
		}
		
		for (Fragment f : manager.getFragments()) {
			if (f instanceof AutoUpdateIconContext) {
				((AutoUpdateIconContext)f).onAutoUpdateChanged(rowId, value);
			}
		}
	}
}
