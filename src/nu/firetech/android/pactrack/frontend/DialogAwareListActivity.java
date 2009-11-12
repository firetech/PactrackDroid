/*
 * Copyright (C) 2009 Joakim Andersson
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import nu.firetech.android.pactrack.common.ContextListener;

import android.app.ListActivity;
import android.os.Bundle;

public class DialogAwareListActivity extends ListActivity {
	private HashMap<Class<?>,Dialog> mDialogs = new HashMap<Class<?>,Dialog>();
	
	static ArrayList<ContextListener> sListeners = new ArrayList<ContextListener>();
	
	private static final String KEY_DIALOGS = "dialog_classes";
	private static final String KEY_DIALOG_DATA = "dialog_data_%d";

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			String[] dialogs = savedInstanceState.getStringArray(KEY_DIALOGS);
			for (int i = 0; i < dialogs.length; i++) {
				try {
					Class<?> c = Class.forName(dialogs[i]); 
					Method m = getShowMethodForClass(c);
					m.invoke(c, this, savedInstanceState.getBundle(String.format(KEY_DIALOG_DATA, i)));
				} catch (Exception e) {} // Ignore errors, null pointers for example.
			}
		}
		
    	if (!sListeners.isEmpty()) {
    		for (ContextListener l : sListeners) {
    			try {
    			    l.onContextChange(this);
    			} catch (Exception e) {} // Ignore errors, null pointers for example.
    		}
    	}
	}
	
	protected Method getShowMethodForClass(Class<?> c) throws NoSuchMethodException {
		return c.getMethod("show", DialogAwareListActivity.class, Bundle.class);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		String[] dialogClasses = new String[mDialogs.size()];
		Dialog[] dialogs = mDialogs.values().toArray(new Dialog[0]);
		for (int i = 0; i < mDialogs.size(); i++) {
			Dialog d = dialogs[i];
			dialogClasses[i] = d.getClass().getName();
			outState.putBundle(String.format(KEY_DIALOG_DATA, i), d.getInstanceState());
		}
		outState.putStringArray(KEY_DIALOGS, dialogClasses);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

    	if (!sListeners.isEmpty()) {
    		for (ContextListener l : sListeners) {
    			try {
    			    l.onContextDestroy(this);
    			} catch (Exception e) {} // Ignore errors, null pointers for example.
    		}
    	}
	}

////////////////////////////////////////////////////////////////////////////////
	
	public interface Dialog extends ContextListener {
		//public static void show(DialogAwareListActivity context, Bundle dialogData);
		public Bundle getInstanceState();
	}

	public void addDialog(Dialog dialog) {
		if (mDialogs.containsKey(dialog.getClass())) {
			throw new IllegalArgumentException("A dialog of class " + dialog.getClass() + " is already shown!");
		}
		mDialogs.put(dialog.getClass(), dialog);
		addContextListener(dialog);
	}
	
	public Dialog getDialogByClass(Class<?> klass) {
		return mDialogs.get(klass);
	}

	public void removeDialog(Dialog dialog) {
		mDialogs.remove(dialog.getClass());
		removeContextListener(dialog);
	}

	public void addContextListener(ContextListener listener) {
		sListeners.add(listener);
	}

	public void removeContextListener(ContextListener listener) {
		sListeners.remove(listener);
	}
}
