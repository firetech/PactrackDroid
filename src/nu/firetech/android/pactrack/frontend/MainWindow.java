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
import nu.firetech.android.pactrack.backend.ParcelUpdater;
import nu.firetech.android.pactrack.backend.ParcelXMLParser;
import nu.firetech.android.pactrack.backend.ServiceStarter;
import nu.firetech.android.pactrack.common.ContextListener;
import nu.firetech.android.pactrack.common.Error;
import nu.firetech.android.pactrack.common.RefreshContext;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class MainWindow extends BarcodeListeningListActivity implements RefreshContext, ParcelOptionsMenu.UpdateableView {
	private static final int ABOUT_ID = Menu.FIRST;
	private static final int REFRESH_ID = Menu.FIRST + 1;
	private static final int SETTINGS_ID = Menu.FIRST + 2;
	private static final int RENAME_ID = Menu.FIRST + 3;
	private static final int DELETE_ID = Menu.FIRST + 4;
	
	private static String sAboutMessage = null;

	private ParcelDbAdapter mDbAdapter;
	private AlertDialog mAboutDialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_window);

		mDbAdapter = new ParcelDbAdapter(this);
		mDbAdapter.open();

		Button addButton = (Button)findViewById(R.id.add_parcel);
		addButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				new ParcelIdDialog(MainWindow.this, null, mDbAdapter).show();
			}
		});

		// Start service if it isn't running (not entirely fool-proof, but works)
		if (ServiceStarter.getCurrentInterval() == -1) {
			ServiceStarter.startService(this, mDbAdapter);
		}
		
		if (sAboutMessage == null) {
			String spacer = "\n\n";
			
			sAboutMessage = new StringBuilder(getString(R.string.app_name))
			.append(" - ")
			.append(getString(R.string.version_name))
			.append(spacer)
			.append("Copyright (C) 2013 Joakim Andersson")
			.append("\n")
			.append("Copyright (C) 2013 blunden")
			.append(spacer)
			.append("This program comes with ABSOLUTELY NO WARRANTY.\nThis is free software, licensed under the GNU General Public License; version 2.")
			.toString();
		}
		
		mAboutDialog = new AlertDialog.Builder(this)
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

		refreshDone();
		registerForContextMenu(getListView());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDbAdapter.close();
	}

	@Override
	public Handler getProgressHandler() {
		return ((RefreshDialog)getDialogByClass(RefreshDialog.class)).getProgressHandler();
	}

	@Override
	public void startRefreshProgress(int maxValue, ContextListener contextListener) {
		RefreshDialog.show(this, maxValue);
		addContextListener(contextListener);
	}

	@Override
	public void refreshDone() {
		Cursor parcelCursor = mDbAdapter.fetchAllParcels(false);
		startManagingCursor(parcelCursor);

		String[] from = new String[]{ParcelDbAdapter.KEY_CUSTOM, ParcelDbAdapter.KEY_CUSTOMER, ParcelDbAdapter.KEY_STATUSCODE};
		int[] to = new int[]{android.R.id.text1, android.R.id.text2, android.R.id.icon};

		SimpleCursorAdapter parcels = 
			new SimpleCursorAdapter(this, R.layout.parcel_row, parcelCursor, from, to);

		parcels.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (view instanceof ImageView && view.getId() == android.R.id.icon) {
					((ImageView)view).setImageResource(getStatusImage(cursor, columnIndex));
					if (cursor.getInt(cursor.getColumnIndexOrThrow(ParcelDbAdapter.KEY_AUTO)) == 1) {
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

		setListAdapter(parcels);
	}

	@Override
	public boolean showsNews() {
		return false;
	}
	
	@Override
	public void updateAutoUpdateView(int position, boolean value) {
		ImageView icon = (ImageView)getListView().getChildAt(position).findViewById(android.R.id.icon);
		icon.getDrawable().mutate().setAlpha((value ? 255 : 70));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, REFRESH_ID, 0, R.string.menu_refresh_all).setIcon(R.drawable.ic_menu_refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(0, ABOUT_ID, 0, R.string.menu_about).setIcon(android.R.drawable.ic_menu_info_details);
		menu.add(0, SETTINGS_ID, 0, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
		case ABOUT_ID:
			mAboutDialog.show();
			return true;
		case REFRESH_ID:
			//Defer automatic update at least another half interval
			if (ServiceStarter.getCurrentInterval() > 0) {
				ServiceStarter.startService(this, mDbAdapter);
			}
			ParcelUpdater.updateAll(false, this, mDbAdapter);
			return true;
		case SETTINGS_ID:
			startActivity(new Intent(this, ConfigView.class));
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		new ParcelOptionsMenu(this, menu, info.id, info.position, mDbAdapter, this);
		menu.add(0, RENAME_ID, 0, R.string.menu_rename);
		menu.add(0, DELETE_ID, 0, R.string.menu_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info;
		switch(item.getItemId()) {
		case RENAME_ID:
			info = (AdapterContextMenuInfo) item.getMenuInfo();
			ParcelIdDialog.show(this, info.id, mDbAdapter);
			return true;
		case DELETE_ID:
			info = (AdapterContextMenuInfo) item.getMenuInfo();
			deleteParcel(info.id, this, mDbAdapter, new Runnable() {
				@Override
				public void run() {
					refreshDone();					
				}
			});
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, ParcelView.class).putExtra(ParcelDbAdapter.KEY_ROWID, id);
		startActivity(i);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, 
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		refreshDone();
	}

////////////////////////////////////////////////////////////////////////////////
	
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
