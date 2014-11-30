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

import java.util.List;

import nu.firetech.android.pactrack.R;
import nu.firetech.android.pactrack.backend.ParcelDbAdapter;
import nu.firetech.android.pactrack.backend.ServiceStarter;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class ConfigView extends PreferenceActivity {
	
	Toolbar mToolbar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        LinearLayout content = (LinearLayout) root.getChildAt(0);
        LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.config_view, null);

        root.removeAllViews();
        toolbarContainer.addView(content);
        root.addView(toolbarContainer);

        mToolbar = (Toolbar) toolbarContainer.findViewById(R.id.toolbar);
        mToolbar.setTitle(getTitle());
        mToolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	@Override
	protected boolean isValidFragment(String fragmentName)
	{
		Class<?> validFragments[] = new Class<?>[] {
				AutoUpdatesFragment.class,
				NotificationFragment.class,
				NotificationLightFragment.class
		};
		for (Class<?> fragment : validFragments) {
			if (fragment.getName().equals(fragmentName)) {
				return true;
			}
		}
		
		return false;
	}

	public static class AutoUpdatesFragment extends PreferenceFragment {
		private ParcelDbAdapter mDbAdapter;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			addPreferencesFromResource(R.xml.auto_updates_preferences);

			mDbAdapter = new ParcelDbAdapter(getActivity());
			mDbAdapter.open();

			ListPreference checkIntervalPref = (ListPreference)findPreference(getString(R.string.key_check_interval));

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
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			mDbAdapter.close();
		}
	}

	public static class NotificationFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			addPreferencesFromResource(R.xml.notification_preferences);
		}
	}

	public static class NotificationLightFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			addPreferencesFromResource(R.xml.notification_light_preferences);
		}
	}
}
