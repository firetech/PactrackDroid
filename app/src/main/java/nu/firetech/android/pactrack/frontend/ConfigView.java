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

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.MenuItem;

import nu.firetech.android.pactrack.R;
import nu.firetech.android.pactrack.backend.ParcelDbAdapter;
import nu.firetech.android.pactrack.backend.ParcelJsonParser;
import nu.firetech.android.pactrack.backend.ServiceStarter;

public class ConfigView extends AppCompatActivity {

	Toolbar mToolbar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new MainConfigFragment())
                .commit();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
            onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    @Override
    public void onBackPressed() {
        // We need to do this ourselves because we're using AppCompatActivity with the standard FragmentManager.
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            finish();
        }
    }

	public static class MainConfigFragment extends PreferenceFragment {
		private ParcelDbAdapter mDbAdapter;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.preferences);

			mDbAdapter = new ParcelDbAdapter(getActivity());
			mDbAdapter.open();

			ListPreference checkIntervalPref = (ListPreference)findPreference(getString(R.string.key_check_interval));
			final EditTextPreference privateApikeyPref = (EditTextPreference)findPreference(getString(R.string.key_private_apikey));
            Preference notificationLightAdvPref = findPreference(getString(R.string.key_notify_light_advanced));

			if (privateApikeyPref.getText().equals("") && Long.parseLong(checkIntervalPref.getValue()) < 60) {
				checkIntervalPref.setValue("60");
			}

			ConnectivityManager cm = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

			//We still have to do this for pre-ICS Android, and the "new" way has always been handled in ParcelUpdater.
			@SuppressWarnings("deprecation")
			boolean backgroundDataAllowed = cm.getBackgroundDataSetting();

			checkIntervalPref.setEnabled(backgroundDataAllowed);
			if (backgroundDataAllowed) {
				checkIntervalPref.setSummary(R.string.check_interval_summary);
			} else {
				checkIntervalPref.setSummary(R.string.check_interval_summary_disabled);
			}

			checkIntervalPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					long newInterval = Long.parseLong((String) newValue) * DateUtils.MINUTE_IN_MILLIS;
					if (newInterval < 60 * DateUtils.MINUTE_IN_MILLIS && (privateApikeyPref.getText().equals(""))) {
						new AlertDialog.Builder(getActivity())
								.setTitle(R.string.private_apikey_needed_title)
								.setMessage(R.string.private_apikey_needed_text)
								.setPositiveButton(R.string.ok, new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								})
								.setNegativeButton(R.string.go_get_apikey, new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.postnord_developer_page)))
												.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
									}
								})
								.create()
								.show();
						return false;
					}
					if (newInterval != ServiceStarter.getCurrentInterval()) {
						ServiceStarter.startService(getActivity(), mDbAdapter, newInterval);
					}

					return true;
				}
			});

			//Backwards compatibility to old method of disabling automatic updates
			if (checkIntervalPref.getValue().equals("0")) {
				checkIntervalPref.setValue(getString(R.string.check_interval_default));

				CheckBoxPreference autoUpdatesEnabledPref = (CheckBoxPreference)findPreference(
						getString(R.string.key_auto_updates));
				autoUpdatesEnabledPref.setChecked(false);
			}

			privateApikeyPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String newText = (String)newValue;
					if (!newText.matches("^([0-9a-f]{32}|)$")) {
						new AlertDialog.Builder(getActivity())
								.setTitle(R.string.private_apikey_error_title)
								.setMessage(R.string.private_apikey_error_text)
								.setPositiveButton(R.string.ok, new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								})
								.setNegativeButton(R.string.go_get_apikey, new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.postnord_developer_page)))
												.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
									}
								})
								.create()
								.show();
						return false;
                    }
					ParcelJsonParser.resetApiKey();
					return true;
				}
			});

            notificationLightAdvPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getFragmentManager().beginTransaction()
                            .replace(android.R.id.content, new NotificationLightFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                }
            });
		}

        @Override
        public void onResume() {
            super.onResume();

            getActivity().setTitle(R.string.menu_settings);
        }

		@Override
		public void onDestroy() {
			super.onDestroy();
			mDbAdapter.close();
		}
	}

    public static class NotificationLightFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.notification_light_preferences);
        }

        @Override
        public void onResume() {
            super.onResume();

            getActivity().setTitle(R.string.notify_light_advanced_title);
        }
    }
}
