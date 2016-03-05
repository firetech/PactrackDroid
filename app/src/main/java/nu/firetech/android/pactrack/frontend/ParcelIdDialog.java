/*
 * Copyright (C) 2016 Joakim Tufvegren
 * Copyright (C) 2016 blunden
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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.InputType;
import android.text.Selection;
import android.text.method.NumberKeyListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ParcelIdDialog extends DialogFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {
	private static final int INITIAL_LOADER_ID = 1;
	
	private static final String KEY_SELECTION_START = "selection_start";
	private static final String KEY_SELECTION_END = "selection_end";
	private static final String KEY_FOCUSED_FIELD = "focused_field";
	
	private EditText mParcelText;
	private EditText mNameText;
	private Long mRowId;
	private ParcelDbAdapter mDbAdapter;
	private View mFocusedView;
	private int mInitialSelectionStart;
	private int mInitialSelectionEnd;

	private ParentActivity mParent;
	private AlertDialog mErrorDialog;

	public static void create(final FragmentManager manager, Long rowId) {
		ParcelIdDialog d = new ParcelIdDialog();
		d.mRowId = rowId;
		d.show(manager, ParcelIdDialog.class.getName());
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);

		dialog.setContentView(R.layout.parcel_id_dialog);
		dialog.setTitle(R.string.menu_add_parcel);
		
		mErrorDialog = new AlertDialog.Builder(getActivity())
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
		
		mDbAdapter = new ParcelDbAdapter(getActivity()).open();

		mParcelText = (EditText) dialog.findViewById(R.id.parcelid);
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
		mNameText = (EditText) dialog.findViewById(R.id.parcelname);

		ImageButton scanButton = (ImageButton) dialog.findViewById(R.id.barcode);
		scanButton.setOnClickListener(new ScanButtonListener());
		Button cancelButton = (Button) dialog.findViewById(R.id.cancel);
		cancelButton.setOnClickListener(new ClosingButtonListener());
		Button okButton = (Button) dialog.findViewById(R.id.ok);
		okButton.setOnClickListener(new OkListener());

		if (savedInstanceState != null && savedInstanceState.containsKey(ParcelDbAdapter.KEY_ROWID)) {
			mRowId = savedInstanceState.getLong(ParcelDbAdapter.KEY_ROWID);
		}

		boolean loadParcel = false;

		String parcelInitialText = "";
		if (savedInstanceState != null && savedInstanceState.containsKey(ParcelDbAdapter.KEY_PARCEL)) {
			parcelInitialText = savedInstanceState.getString(ParcelDbAdapter.KEY_PARCEL);
		} else if (mRowId != null) {
			loadParcel = true;
			parcelInitialText = getString(R.string.loading);
			mParcelText.setEnabled(false);
		}
		mParcelText.setText(parcelInitialText);

		String nameInitialText = "";
		if (savedInstanceState != null && savedInstanceState.containsKey(ParcelDbAdapter.KEY_NAME)) {
			nameInitialText = savedInstanceState.getString(ParcelDbAdapter.KEY_NAME);
		} else if (mRowId != null) {
			loadParcel = true;
			nameInitialText = getString(R.string.loading);
			mNameText.setEnabled(false);
		}
		mNameText.setText(nameInitialText);
		
		mFocusedView = null;
		if (savedInstanceState != null && savedInstanceState.containsKey(KEY_FOCUSED_FIELD)) {
			mFocusedView = dialog.findViewById(savedInstanceState.getInt(KEY_FOCUSED_FIELD));
			
			mInitialSelectionStart = mInitialSelectionEnd = 0;
			if (mFocusedView instanceof EditText &&
					savedInstanceState.containsKey(KEY_SELECTION_START) &&
					savedInstanceState.containsKey(KEY_SELECTION_END)) {
				mInitialSelectionStart = savedInstanceState.getInt(KEY_SELECTION_START);
				mInitialSelectionEnd = savedInstanceState.getInt(KEY_SELECTION_END);
				
				Selection.setSelection(((EditText)mFocusedView).getText(), mInitialSelectionStart, mInitialSelectionEnd);
			}
			mFocusedView.requestFocus();
		}
		
		if (loadParcel) {
			getLoaderManager().initLoader(INITIAL_LOADER_ID, null, this);
		}
		
		return dialog;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		try {
            mParent = (ParentActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ParentActivity");
        }
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mDbAdapter.close();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (mRowId != null) {
			outState.putLong(ParcelDbAdapter.KEY_ROWID, mRowId);
		}
		
		outState.putString(ParcelDbAdapter.KEY_PARCEL, mParcelText.getText().toString());
		outState.putString(ParcelDbAdapter.KEY_NAME, mNameText.getText().toString());
		
		View focusedView = getDialog().getCurrentFocus();
		if (focusedView != null && focusedView.getId() != View.NO_ID) {
			outState.putInt(KEY_FOCUSED_FIELD, focusedView.getId());
			if (focusedView instanceof EditText) {
				outState.putInt(KEY_SELECTION_START, Selection.getSelectionStart(((EditText) focusedView).getText()));
				outState.putInt(KEY_SELECTION_END, Selection.getSelectionEnd(((EditText) focusedView).getText()));
			}
		}
	}

	private class ScanButtonListener implements android.view.View.OnClickListener {
		@Override
		public void onClick(View v) {
			IntentIntegrator.initiateScan(ParcelIdDialog.this,
					R.string.install_barcode_title,
					R.string.install_barcode_message,
					R.string.yes,
					R.string.no);
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
			if (parcel.length() < 9) {
				mErrorDialog.show();
				return;
			}

			String name = mNameText.getText().toString();
			if (mRowId == null) {
				mRowId = mDbAdapter.addParcel(parcel, name);
				mParent.showAndRefreshParcel(mRowId);
			} else if (mDbAdapter.changeParcelIdName(mRowId, parcel, name)) {
				mParent.showAndRefreshParcel(mRowId);
			}
			super.onClick(v);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null) {
			mParcelText.setText(scanResult.getContents());
		}
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case INITIAL_LOADER_ID:
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
			}
			if (!mNameText.isEnabled()) {
				mNameText.setText(cursor.getString(cursor
					.getColumnIndexOrThrow(ParcelDbAdapter.KEY_NAME)));
				mNameText.setEnabled(true);
			}
			if (mFocusedView != null) {
				if (mFocusedView instanceof EditText) {
					Selection.setSelection(((EditText) mFocusedView).getText(), mInitialSelectionStart, mInitialSelectionEnd);
				}
				mFocusedView.requestFocus();
			}
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {}


	////////////////////////////////////////////////////////////////////////////////
	
	public interface ParentActivity {
        void showAndRefreshParcel(long rowId);
	}
}
