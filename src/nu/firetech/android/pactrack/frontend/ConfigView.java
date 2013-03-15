/*
 * Copyright (C) 2011 Joakim Andersson
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

import com.sonyericsson.extras.liveview.plugins.PluginConstants;

import nu.firetech.android.pactrack.R;
import nu.firetech.android.pactrack.backend.ParcelDbAdapter;
import nu.firetech.android.pactrack.backend.ServiceStarter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.text.format.DateUtils;
import android.text.method.NumberKeyListener;

public class ConfigView extends PreferenceActivity {
	private ParcelDbAdapter mDbAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setTitle(getString(R.string.menu_settings));
		addPreferencesFromResource(R.layout.preferences);
		
		ListPreference checkIntervalPref = (ListPreference)findPreference(getString(R.string.key_check_interval));
		checkIntervalPref.setEnabled(((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE)).getBackgroundDataSetting());
		if (!checkIntervalPref.isEnabled()) {
			checkIntervalPref.setSummary(R.string.check_interval_summary_disabled);
		} else {
			checkIntervalPref.setSummary(R.string.check_interval_summary);
		}
		
		mDbAdapter = new ParcelDbAdapter(this);
		mDbAdapter.open();
		
		checkIntervalPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				long newInterval = Long.parseLong((String) newValue) * DateUtils.MINUTE_IN_MILLIS;
				if (newInterval != ServiceStarter.getCurrentInterval()) {
					ServiceStarter.startService(ConfigView.this, mDbAdapter, newInterval);
				}
				
				return true;
			}
		});
		
		EditTextPreference lightColorPref = (EditTextPreference)findPreference(getString(R.string.key_notify_light_color));
		lightColorPref.getEditText().setFilters(new InputFilter[] { new InputFilter.LengthFilter(6) });
		lightColorPref.getEditText().setKeyListener(new NumberKeyListener() {
			private char[] acceptedChars = null;

			@Override
			protected char[] getAcceptedChars() {
				if (acceptedChars == null) {
					acceptedChars = new char[] {
							'0','1','2','3','4','5','6','7','8','9',
							'A','B','C','D','E','F',
							'a','b','c','d','e','f'
					};
				}

				return acceptedChars;
			}

			@Override
			public int getInputType() {
				return InputType.TYPE_CLASS_NUMBER;
			}
		});
		
		EditTextPreference lightOntimePref = (EditTextPreference)findPreference(getString(R.string.key_notify_light_ontime));
		lightOntimePref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		EditTextPreference lightOfftimePref = (EditTextPreference)findPreference(getString(R.string.key_notify_light_offtime));
		lightOfftimePref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		
		CheckBoxPreference liveViewPref = (CheckBoxPreference)findPreference(getString(R.string.key_liveview_announce));
		Intent i = new Intent(PluginConstants.LIVEVIEW_SERVICE_BIND_INTENT);
		List<ResolveInfo> list = getPackageManager().queryIntentServices(i, PackageManager.MATCH_DEFAULT_ONLY);  
        if (list.size() == 0) {
        	liveViewPref.setEnabled(false);
        	liveViewPref.setChecked(false);
        	liveViewPref.setSummary(R.string.liveview_enabled_summary_disabled);
        } else {
        	liveViewPref.setEnabled(true);
        	liveViewPref.setSummary(R.string.liveview_enabled_summary);
        }
	}
	

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDbAdapter.close();
	}
}
