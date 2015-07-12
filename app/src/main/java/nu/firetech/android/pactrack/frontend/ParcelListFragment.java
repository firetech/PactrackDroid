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
import nu.firetech.android.pactrack.backend.ParcelUpdater;
import nu.firetech.android.pactrack.backend.Preferences;
import nu.firetech.android.pactrack.backend.ServiceStarter;
import nu.firetech.android.pactrack.common.RefreshContext;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ParcelListFragment extends ListFragment implements
		RefreshContext, AutoUpdateIconContext, LoaderManager.LoaderCallbacks<Cursor> {
	private static final int PARCELS_LOADER_ID = 0;

	private static String sAboutMessage = null;

	private ParcelDbAdapter mDbAdapter;
	private AlertDialog mAboutDialog;
	private SimpleCursorAdapter mAdapter;
	private ParentActivity mParent;
	private Long mSelectedId = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.parcel_list, container, false);
		
		Context context = view.getContext();

		FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ParcelIdDialog.create(getFragmentManager(), null);
			}
		});

		if (sAboutMessage == null) {
			String spacer = "\n\n";

			String versionName = "Huh?";
			try {
				versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {}

			sAboutMessage = new StringBuilder(getString(R.string.app_name))
			.append(" - ")
			.append(versionName)
			.append(spacer)
			.append("Copyright (C) 2014 Joakim Andersson")
			.append("\n")
			.append("Copyright (C) 2014 blunden")
			.append(spacer)
			.append("This program comes with ABSOLUTELY NO WARRANTY.\nThis is free software, licensed under the GNU General Public License; version 2.")
			.toString();
		}
		
		mAboutDialog = new AlertDialog.Builder(context)
		.setTitle(R.string.menu_about)
		.setMessage(sAboutMessage)
		.setPositiveButton(R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.setNegativeButton(R.string.go_homepage, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.home_page)))
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			}
		})
		.create();
		
		String[] from = new String[]{ParcelDbAdapter.KEY_CUSTOM, ParcelDbAdapter.KEY_CUSTOMER, ParcelDbAdapter.KEY_STATUSCODE};
		int[] to = new int[]{android.R.id.text1, android.R.id.text2, android.R.id.icon};

		final Preferences prefs = Preferences.getPreferences(context);
		
		mAdapter = new SimpleCursorAdapter(context, R.layout.parcel_row, null, from, to, 0);
		mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (view instanceof ImageView && view.getId() == android.R.id.icon) {
					((ImageView)view).setImageResource(UICommon.getStatusImage(cursor, columnIndex));
					if (prefs.getCheckInterval() == 0 ||
							cursor.getInt(cursor.getColumnIndexOrThrow(ParcelDbAdapter.KEY_AUTO)) == 1) {
						((ImageView)view).getDrawable().mutate().setAlpha(255);
					} else {
						((ImageView)view).getDrawable().mutate().setAlpha(70);
					}
					return true;
				} else {
					return false;
				}
			}
		});
		setListAdapter(mAdapter);
		
		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	    registerForContextMenu(getListView());
	}
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

		mDbAdapter = new ParcelDbAdapter(activity);
		mDbAdapter.open();

        try {
            mParent = (ParentActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ParentActivity");
        }
    }
	
    @Override
    public void onDetach() {
        super.onDetach();
		mDbAdapter.close();
    }

    @Override
    public void onStart() {
        super.onStart();

        // When in two-pane layout, set to highlight the selected list item
        if (getFragmentManager().findFragmentById(R.id.list_frag) != null) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
    }

	@Override
	public void onResume() {
		super.onResume();
		refreshDone();
        mParent.onListResume();
	}

	@Override
	public Handler startRefreshProgress(int maxValue) {
		return RefreshDialog.show(getActivity(), maxValue);
	}

	@Override
	public void refreshDone() {
		LoaderManager lm = getLoaderManager();
		if (lm.getLoader(PARCELS_LOADER_ID) == null) {
			lm.initLoader(PARCELS_LOADER_ID, null, this);
		} else {
			lm.restartLoader(PARCELS_LOADER_ID, null, this);
		}
	}

	@Override
	public boolean showsNews() {
		return false;
	}
	
	public void selectRowItem(Long rowId) {
		mSelectedId = rowId;
		
		if (rowId != null) {
			for (int i = 0; i < mAdapter.getCount(); i++) {
				if (rowId == mAdapter.getItemId(i)) {
					getListView().setItemChecked(i, true);
					return;
				}
			}
		}
		
		//No match
		getListView().setItemChecked(getListView().getCheckedItemPosition(), false);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.main_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_about:
			mAboutDialog.show();
			return true;
		case R.id.action_refresh_all:
			//Defer automatic update at least another half interval
			if (ServiceStarter.getCurrentInterval() > 0) {
				ServiceStarter.startService(getActivity(), mDbAdapter);
			}
			ParcelUpdater.updateAll(false, this, mDbAdapter);
			return true;
		case R.id.action_settings:
			Intent intent = new Intent(getActivity(), ConfigView.class);
			startActivity(intent);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.parcel_menu, menu);
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		
		boolean enabled = (Preferences.getPreferences(getActivity()).getCheckInterval() > 0);
		MenuItem autoInclude = menu.findItem(R.id.action_auto_include);
		autoInclude.setEnabled(enabled);
		autoInclude.setChecked(enabled && mDbAdapter.getAutoUpdate(info.id));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId()) {
		case R.id.action_delete:
			UICommon.deleteParcel(info.id, getActivity(), mDbAdapter, new Runnable() {
				@Override
				public void run() {
					refreshDone();
				}
			});
			return true;
		case R.id.action_edit:
			ParcelIdDialog.create(getFragmentManager(), info.id);
			return true;
		case R.id.action_auto_include:
			mDbAdapter.setAutoUpdate(info.id, !item.isChecked());
			mParent.onAutoUpdateChanged(info.id, !item.isChecked());
			return true;
		case R.id.action_refresh:
			mParent.showParcel(info.id, true);
			return true;
		}
		
		return super.onContextItemSelected(item);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		mParent.showParcel(id, false);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, 
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		refreshDone();
	}

	////////////////////////////////////////////////////////////////////////////////

	@Override
	public void onAutoUpdateChanged(long rowId, boolean value) {
		refreshDone();
	}

	////////////////////////////////////////////////////////////////////////////////

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case PARCELS_LOADER_ID:
			return mDbAdapter.getAllParcelsLoader(false);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		switch (loader.getId()) {
		case PARCELS_LOADER_ID:
			mAdapter.swapCursor(cursor);
			selectRowItem(mSelectedId);
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case PARCELS_LOADER_ID:
			mAdapter.swapCursor(null);
			break;
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	
	public interface ParentActivity {
        public void showParcel(long rowId, boolean forceRefresh);
        public void onListResume();
        public void onAutoUpdateChanged(long rowId, boolean value);
    }
	
}
