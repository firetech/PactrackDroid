/*
 * Copyright (C) 2014 Joakim Andersson
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
import nu.firetech.android.pactrack.backend.ParcelUpdater;
import nu.firetech.android.pactrack.backend.Preferences;
import nu.firetech.android.pactrack.common.Error;
import nu.firetech.android.pactrack.common.RefreshContext;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ParcelDetailsFragment extends ListFragment implements
		RefreshContext, LoaderManager.LoaderCallbacks<Cursor> {
	private static final int PARCEL_LOADER_ID = 1;
	private static final int EVENTS_LOADER_ID = 2;
	private static final int REFRESH_LOADER_ID = 3;

	private static final String TAG = "<PactrackDroid> ParcelView";
	
	public static final String FORCE_REFRESH = "force_update";

	private Long mRowId = null;
	private ParcelDbAdapter mDbAdapter;
	private LinearLayout mExtended;
	private Button mToggleButton;
	private boolean mExtendedShowing = false;
	private int errorShown = Error.NONE;
	private SimpleCursorAdapter mEventsAdapter;
	
	private ParentActivity mParent;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.parcel_view, container, false);

		mExtended = (LinearLayout)view.findViewById(R.id.extended);
		mToggleButton = (Button)view.findViewById(R.id.extended_toggle);

		mToggleButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mExtendedShowing = !mExtendedShowing;
				if (mExtendedShowing) {
					mExtended.setVisibility(View.VISIBLE);
					mToggleButton.setText(R.string.hide_extended);
				} else {
					mExtended.setVisibility(View.GONE);
					mToggleButton.setText(R.string.show_extended);
				}
			}
		});
		
		if (mExtendedShowing) {
			mExtended.setVisibility(View.VISIBLE);
			mToggleButton.setText(R.string.hide_extended);
		}


		String[] from = new String[]{ParcelDbAdapter.KEY_CUSTOM, ParcelDbAdapter.KEY_DESC};
		int[] to = new int[]{android.R.id.title, android.R.id.text1};
		
		mEventsAdapter = new SimpleCursorAdapter(view.getContext(), R.layout.event_row, null, from, to, 0);
		setListAdapter(mEventsAdapter);
		
		Bundle extras = getArguments();
		if (extras != null) {
			if (mRowId == null && extras.containsKey(ParcelDbAdapter.KEY_ROWID)) {
				mRowId = extras.getLong(ParcelDbAdapter.KEY_ROWID);
			}
			if (extras.containsKey(FORCE_REFRESH)) {
				doRefresh();
			}
		}
		
		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		return view;
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
	public void onResume() {
		super.onResume();
		refreshDone();
	}
	
	public void doRefresh() {
		LoaderManager lm = getLoaderManager();
		if (lm.getLoader(REFRESH_LOADER_ID) == null) {
			lm.initLoader(REFRESH_LOADER_ID, null, this);
		} else {
			lm.restartLoader(REFRESH_LOADER_ID, null, this);
		}
	}
	
	public void switchParcel(long newId, boolean forceRefresh) {
		if (mExtendedShowing && newId != mRowId) {
			mExtended.setVisibility(View.GONE);
			mToggleButton.setText(R.string.show_extended);
			mExtendedShowing = false;
		}
		mRowId = newId;
		
		refreshDone();
		
		if (forceRefresh) {
			doRefresh();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.parcel_menu, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		boolean enabled = (Preferences.getPreferences(getActivity()).getCheckInterval() > 0);
		MenuItem autoInclude = menu.findItem(R.id.action_auto_include);
		autoInclude.setEnabled(enabled);
		autoInclude.setChecked(enabled && mDbAdapter.getAutoUpdate(mRowId));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_delete:
			UICommon.deleteParcel(mRowId, getActivity(), mDbAdapter, new Runnable() {
				@Override
				public void run() {
					mParent.onCurrentParcelRemoved();
				}
			});
			return true;
		case R.id.action_edit:
			ParcelIdDialog.create(getFragmentManager(), mRowId);
			return true;
		case R.id.action_auto_include:
			mDbAdapter.setAutoUpdate(mRowId, !item.isChecked());
			mParent.onAutoUpdateChanged(mRowId, !item.isChecked());
			return true;
		case R.id.action_refresh:
			errorShown = Error.NONE;
			doRefresh();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public Handler startRefreshProgress(int maxValue) {
		return RefreshDialog.show(getActivity(), maxValue);
	}
	
	@Override
	public void refreshDone() {
		int[] loaders = { PARCEL_LOADER_ID, EVENTS_LOADER_ID };
		
		LoaderManager lm = getLoaderManager();
		for (int loader_id : loaders) {
			if (lm.getLoader(loader_id) == null) {
				lm.initLoader(loader_id, null, this);
			} else {
				lm.restartLoader(loader_id, null, this);
			}
		}
	}

	@Override
	public boolean showsNews() {
		return true;
	}

	private void updateView(Cursor parcel) {
		((NotificationManager)getActivity().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(mRowId.hashCode());
		
		boolean isFullscreen = (getFragmentManager().findFragmentById(R.id.details_frag) == null);
		
		try {
			int error = parcel.getInt(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_ERROR));
			
			String status = parcel.getString(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_STATUS));
			
			if (error != Error.NONE && errorShown != error) {
				switch(error) {
				case Error.NOT_FOUND:
					status = getString(R.string.parcel_error_not_found);
					break;
				case Error.MULTI_PARCEL:
					status = getString(R.string.parcel_error_multi_parcel);
					break;
				case Error.SERVER:
					status = getString(R.string.parcel_error_server);
					break;
				default:
					status = getString(R.string.parcel_error_unknown, error);
				}
				
				AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.parcel_problem)
				.setIconAttribute(android.R.attr.alertDialogIcon);
				
				if (isFullscreen) {
					dialog
					.setMessage(getString(R.string.parcel_error_message_question, status))
					.setPositiveButton(R.string.yes, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					})
					.setNegativeButton(R.string.no, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							getFragmentManager().popBackStack();
						}
					});
				} else {
					dialog
					.setMessage(getString(R.string.parcel_error_message, status))
					.setNegativeButton(R.string.ok, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
				}
				
				dialog.create().show();
			}
			
			errorShown = error;

			String parcelId = parcel.getString(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_PARCEL));
			String parcelName = parcel.getString(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_NAME));
			if (parcelName == null) {
				parcelName = getString(R.string.generic_parcel_name, parcelId);
			}

			if (isFullscreen) {
				getActivity().setTitle(parcelName);
			}
			
			String lastUpdate = null, lastOkUpdate = null;
			int lastUpdateIndex = parcel.getColumnIndex(ParcelDbAdapter.KEY_UPDATE);
			if (lastUpdateIndex >= 0) {
				lastUpdate = parcel.getString(lastUpdateIndex);
			}
			int lastOkUpdateIndex = parcel.getColumnIndex(ParcelDbAdapter.KEY_OK_UPDATE);
			if (lastOkUpdateIndex >= 0) {
				lastOkUpdate = parcel.getString(lastOkUpdateIndex);
			}
			if (lastUpdate != null && lastUpdate.equals(lastOkUpdate)) {
				lastOkUpdate = getString(R.string.same_time);
			}

			findTextView(R.id.parcelid).setText(parcelId);
			findTextView(R.id.customer).setText(parcel.getString(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_CUSTOMER)));
			findTextView(R.id.sent).setText(parcel.getString(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_SENT)));
			findTextView(R.id.status).setText(status);
			findTextView(R.id.update_info).setText(getString(R.string.update_info_syntax, 
					(lastUpdate == null ? getString(R.string.never) : lastUpdate),
					(lastOkUpdate == null ? getString(R.string.never) : lastOkUpdate)));
			findTextView(R.id.weight).setText(parcel.getString(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_WEIGHT)));
			findTextView(R.id.postal).setText(parcel.getString(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_POSTAL)));
			findTextView(R.id.service).setText(parcel.getString(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_SERVICE)));
			
			((ImageView)getView().findViewById(R.id.status_icon)).setImageResource(
					UICommon.getStatusImage(parcel, parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_STATUSCODE)));
			updateAutoUpdateView(Preferences.getPreferences(getActivity()).getCheckInterval() == 0 ||
					parcel.getInt(parcel.getColumnIndexOrThrow(ParcelDbAdapter.KEY_AUTO)) == 1);
		} catch (Exception e) {
			Log.d(TAG, "Database error", e);
			UICommon.dbErrorDialog(getActivity());
		}
	}

	private TextView findTextView(int resId) {
		return (TextView)getView().findViewById(resId);
	}
	
	public Long getCurrentRowId() {
		return mRowId;
	}

	public void updateAutoUpdateView(boolean value) {
		ImageView icon = (ImageView)getView().findViewById(R.id.status_icon);
		icon.getDrawable().setAlpha((value ? 255 : 70));
		icon.invalidate();
	}

	////////////////////////////////////////////////////////////////////////////////

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
	    switch (id) {
		case PARCEL_LOADER_ID:
	    case REFRESH_LOADER_ID:
			return mDbAdapter.getParcelLoader(mRowId);
	    case EVENTS_LOADER_ID:
			return mDbAdapter.getEventsLoader(mRowId);
	    }
	    return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		switch (loader.getId()) {
		case PARCEL_LOADER_ID:
			updateView(cursor);
			break;
		case EVENTS_LOADER_ID:
			mEventsAdapter.swapCursor(cursor);
			break;
		case REFRESH_LOADER_ID:
			//TODO This can probably be done better...
			final Cursor fCursor = cursor;
			new Handler(getActivity().getMainLooper()){
				@Override
				public void handleMessage(Message m) {
					ParcelUpdater.update(ParcelDetailsFragment.this, fCursor);
				}
			}.sendEmptyMessage(0);
			break;
	    }
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case EVENTS_LOADER_ID:
			mEventsAdapter.swapCursor(null);
			break;
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	
	public interface ParentActivity {
        public void onCurrentParcelRemoved();
        public void onAutoUpdateChanged(long rowId, boolean value);
    }
}
