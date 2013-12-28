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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.text.Selection;
import android.text.method.NumberKeyListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.zxing.integration.android.IntentResult;

public class ParcelIdDialog extends Dialog implements
		DialogAwareListActivity.Dialog, BarcodeListener, LoaderManager.LoaderCallbacks<Cursor> {
	private static final int INITIAL_LOADER_ID = 10;
	private static final int CHANGE_LOADER_ID = 11;
	
	private static final String KEY_PARCEL_SELECTION_START = "parcel_selection_start";
	private static final String KEY_PARCEL_SELECTION_END = "parcel_selection_end";
	private static final String KEY_NAME_SELECTION_START = "name_selection_start";
	private static final String KEY_NAME_SELECTION_END = "name_selection_end";
	private static final String KEY_FOCUSED_FIELD = "focused_field";

	private EditText mParcelText;
	private EditText mNameText;
	private Long mRowId;
	private ParcelDbAdapter mDbAdapter;
	private boolean mCloseDbAdapter = false;
	private String mParcelInitialText;
	private int mParcelInitialSelectionStart;
	private int mParcelInitialSelectionEnd;
	private String mNameInitialText;
	private int mNameInitialSelectionStart;
	private int mNameInitialSelectionEnd;
	private int mFocusedField;

	private AlertDialog mErrorDialog;

	public static void show(final BarcodeListeningListActivity context, Long rowId,
			ParcelDbAdapter dbAdapter) {
		ParcelIdDialog d = new ParcelIdDialog(context, rowId, dbAdapter);
		d.show();
	}

	public static void show(final BarcodeListeningListActivity context, Bundle dialogData) {
		ParcelDbAdapter dbAdapter = new ParcelDbAdapter(context);
		dbAdapter.open();
		ParcelIdDialog d = new ParcelIdDialog(context,
				(dialogData.containsKey(ParcelDbAdapter.KEY_ROWID) ? dialogData.getLong(ParcelDbAdapter.KEY_ROWID) : null),
				dbAdapter);
		d.mParcelInitialText = dialogData.getString(ParcelDbAdapter.KEY_PARCEL);
		d.mParcelInitialSelectionStart = dialogData.getInt(KEY_PARCEL_SELECTION_START);
		d.mParcelInitialSelectionEnd = dialogData.getInt(KEY_PARCEL_SELECTION_END);
		d.mNameInitialText = dialogData.getString(ParcelDbAdapter.KEY_NAME);
		d.mNameInitialSelectionStart = dialogData.getInt(KEY_NAME_SELECTION_START);
		d.mNameInitialSelectionEnd = dialogData.getInt(KEY_NAME_SELECTION_END);
		if (dialogData.containsKey(KEY_FOCUSED_FIELD)) {
			d.mFocusedField = dialogData.getInt(KEY_FOCUSED_FIELD);
		}
		d.mCloseDbAdapter = true;
		d.show();
	}

	public ParcelIdDialog(final BarcodeListeningListActivity context, Long rowId,
			ParcelDbAdapter dbAdapter) {
		super(context);
		setOwnerActivity(context);
		
		mRowId = rowId;
		mDbAdapter = dbAdapter;

		mParcelInitialText = null;
		mParcelInitialSelectionStart = mParcelInitialSelectionEnd = 0;
		mNameInitialText = null;
		mNameInitialSelectionStart = mNameInitialSelectionEnd = 0;
		mFocusedField = R.id.parcelid;

		context.addDialog(this);
		setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				context.removeDialog(ParcelIdDialog.this);
				
				if (mCloseDbAdapter) {
					mDbAdapter.close();
				}
			}
		});

		mErrorDialog = new AlertDialog.Builder(context)
			.setTitle(R.string.id_error_title)
			.setIconAttribute(android.R.attr.alertDialogIcon)
			.setMessage(R.string.id_error_message)
			.setNeutralButton(R.string.ok,
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).create();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.parcel_id_dialog);

		mParcelText = (EditText) findViewById(R.id.parcelid);
		mParcelText.setKeyListener(new NumberKeyListener() {
			private char[] acceptedChars = null;

			@Override
			protected char[] getAcceptedChars() {
				if (acceptedChars == null) {
					acceptedChars = new char[] { '0', '1', '2', '3', '4', '5',
							'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
							'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
							'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
				}

				return acceptedChars;
			}

			@Override
			public int getInputType() {
				return InputType.TYPE_CLASS_TEXT
						| InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
			}
		});
		mNameText = (EditText) findViewById(R.id.parcelname);

		ImageButton scanButton = (ImageButton) findViewById(R.id.barcode);
		scanButton.setOnClickListener(new ScanButtonListener());
		Button cancelButton = (Button) findViewById(R.id.cancel);
		cancelButton.setOnClickListener(new ClosingButtonListener());
		Button okButton = (Button) findViewById(R.id.ok);
		okButton.setOnClickListener(new OkListener());

		if (savedInstanceState != null) {
			mRowId = savedInstanceState.getLong(ParcelDbAdapter.KEY_ROWID);
		}
		if (mRowId == null) {
			setTitle(R.string.menu_add_parcel);
		} else {
			setTitle(R.string.menu_rename);
		}

		boolean loadParcel = false;
		
		if (mRowId != null && mParcelInitialText == null) {
			if (savedInstanceState != null && savedInstanceState.containsKey(ParcelDbAdapter.KEY_PARCEL)) {
				mParcelInitialText = savedInstanceState.getString(ParcelDbAdapter.KEY_PARCEL);
			} else {
				loadParcel = true;
				mParcelInitialText = getContext().getString(R.string.loading);
				mParcelText.setEnabled(false);
			}
			if (savedInstanceState != null && 
					savedInstanceState.containsKey(KEY_PARCEL_SELECTION_START) && 
					savedInstanceState.containsKey(KEY_PARCEL_SELECTION_END)) {
				mParcelInitialSelectionStart = savedInstanceState.getInt(KEY_PARCEL_SELECTION_START);
				mParcelInitialSelectionEnd = savedInstanceState.getInt(KEY_PARCEL_SELECTION_END);
			}
		}

		if (mParcelInitialText == null) {
			mParcelInitialText = "";
		}
		mParcelText.setText(mParcelInitialText);
		Selection.setSelection(mParcelText.getText(), mParcelInitialSelectionStart, mParcelInitialSelectionEnd);

		if (mRowId != null && mNameInitialText == null) {
			if (savedInstanceState != null && savedInstanceState.containsKey(ParcelDbAdapter.KEY_NAME)) {
				mNameInitialText = savedInstanceState.getString(ParcelDbAdapter.KEY_NAME);
			} else {
				loadParcel = true;
				mNameInitialText = getContext().getString(R.string.loading);
				mNameText.setEnabled(false);
			}
			if (savedInstanceState != null && 
					savedInstanceState.containsKey(KEY_NAME_SELECTION_START) && 
					savedInstanceState.containsKey(KEY_NAME_SELECTION_END)) {
				mNameInitialSelectionStart = savedInstanceState.getInt(KEY_NAME_SELECTION_START);
				mNameInitialSelectionEnd = savedInstanceState.getInt(KEY_NAME_SELECTION_END);
			}
		}

		if (mNameInitialText == null) {
			mNameInitialText = "";
		}
		mNameText.setText(mNameInitialText);
		Selection.setSelection(mNameText.getText(), mNameInitialSelectionStart, mNameInitialSelectionEnd);
		
		if (mFocusedField != 0) {
			findViewById(mFocusedField).requestFocus();
		}
		
		if (loadParcel) {
			getOwnerActivity().getLoaderManager().initLoader(INITIAL_LOADER_ID, null, this);
		}
	}

	@Override
	public Bundle getInstanceState() {
		Bundle outState = new Bundle(2);

		if (mRowId != null) {
			outState.putLong(ParcelDbAdapter.KEY_ROWID, mRowId);
		}
		outState.putString(ParcelDbAdapter.KEY_PARCEL, mParcelText.getText().toString());
		outState.putInt(KEY_PARCEL_SELECTION_START, Selection.getSelectionStart(mParcelText.getText()));
		outState.putInt(KEY_PARCEL_SELECTION_END, Selection.getSelectionEnd(mParcelText.getText()));
		outState.putString(ParcelDbAdapter.KEY_NAME, mNameText.getText().toString());
		outState.putInt(KEY_NAME_SELECTION_START, Selection.getSelectionStart(mNameText.getText()));
		outState.putInt(KEY_NAME_SELECTION_END, Selection.getSelectionEnd(mNameText.getText()));
		if (mParcelText.hasFocus()) {
			outState.putInt(KEY_FOCUSED_FIELD, mParcelText.getId());
		} else if (mNameText.hasFocus()) {
			outState.putInt(KEY_FOCUSED_FIELD, mNameText.getId());
		}
		return outState;
	}

	private class ScanButtonListener implements android.view.View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (getOwnerActivity() != null) {
				((BarcodeListeningListActivity)getOwnerActivity()).initiateScan(ParcelIdDialog.this); 	
			}
		}
	}

	private class ClosingButtonListener implements android.view.View.OnClickListener {
		@Override
		public void onClick(View v) {
			ParcelIdDialog.this.dismiss();
		}
	}

	private class OkListener extends ClosingButtonListener {
		@Override
		public void onClick(View v) {
			String parcel = mParcelText.getText().toString();
			String name = mNameText.getText().toString();
			if (parcel.length() < 9) {
				mErrorDialog.show();
				return;
			}
			
			if (mRowId == null) {
				mRowId = mDbAdapter.addParcel(parcel, name);
				doShowAndRefresh();
				super.onClick(v);
			} else {
				getOwnerActivity().getLoaderManager().initLoader(CHANGE_LOADER_ID, null, ParcelIdDialog.this);
			}
		}
	}
	
	private void doShowAndRefresh() {
		if (getOwnerActivity() instanceof ParcelView) {
			((ParcelView) getOwnerActivity()).doRefresh();
		} else {
			Intent i = new Intent(getContext(), ParcelView.class)
					.putExtra(ParcelDbAdapter.KEY_ROWID, mRowId)
					.putExtra(ParcelView.FORCE_REFRESH, true);

			getContext().startActivity(i);
		}
	}

	@Override
	public void onContextChange(Context newContext) {
		((BarcodeListeningListActivity)newContext).setBarcodeListener(this);
	}

	@Override
	public void onContextDestroy(Context oldContext) {
		dismiss();
	}

	@Override
	public void handleBarcode(IntentResult barcode) {
		mParcelText.setText(barcode.getContents());
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case INITIAL_LOADER_ID:
		case CHANGE_LOADER_ID:
			return mDbAdapter.getParcelLoader(mRowId);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		switch (loader.getId()) {
		case INITIAL_LOADER_ID:
			if (!mParcelText.isEnabled()) {
				mParcelText.setText(cursor.getString(cursor
					.getColumnIndexOrThrow(ParcelDbAdapter.KEY_PARCEL)));
				mParcelText.setEnabled(true);
				Selection.setSelection(mParcelText.getText(), mParcelInitialSelectionStart, mParcelInitialSelectionEnd);
			}
			if (!mNameText.isEnabled()) {
				mNameText.setText(cursor.getString(cursor
					.getColumnIndexOrThrow(ParcelDbAdapter.KEY_NAME)));
				mNameText.setEnabled(true);
				Selection.setSelection(mNameText.getText(), mNameInitialSelectionStart, mNameInitialSelectionEnd);
			}
			getOwnerActivity().getLoaderManager().destroyLoader(INITIAL_LOADER_ID);
			break;
		case CHANGE_LOADER_ID:
			String parcel = mParcelText.getText().toString();
			String name = mNameText.getText().toString();
			String oldParcel = cursor.getString(cursor
					.getColumnIndexOrThrow(ParcelDbAdapter.KEY_PARCEL));
			String oldName = cursor.getString(cursor
					.getColumnIndexOrThrow(ParcelDbAdapter.KEY_NAME));
			
			if (!parcel.equals(oldParcel) || (oldName == null && name.length() > 0) || !name.equals(oldName)) {
				mDbAdapter.changeParcelIdName(mRowId, parcel, name);
				doShowAndRefresh();
			}
			getOwnerActivity().getLoaderManager().destroyLoader(CHANGE_LOADER_ID);
			dismiss();
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {}

}
