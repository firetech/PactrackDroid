/*
 * Copyright (c) 2010 Sony Ericsson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 *
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.sonyericsson.extras.liveview.plugins;

import com.sonyericsson.extras.liveview.IPluginServiceCallbackV1;
import com.sonyericsson.extras.liveview.IPluginServiceV1;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Base implementation of the plug-in service.
 */
public abstract class AbstractPluginService extends Service {

    // Template menu icon file name.
    private static final String TEMPLATE_MENU_ICON = "plugin_icon.png";

    // There should only be one instance of the service
    protected static boolean alreadyRunning = false;

    // Plugin name
    protected String mPluginName = null;

    // Current plugin Id
    protected int mPluginId = 0;

    // LiveView adapter reference
    protected LiveViewAdapter mLiveViewAdapter = null;

    // Menu icon that will be shown in LiveView unit
    protected String mMenuIcon = null;

    // Launcher intent for the implemented service
    protected String mServiceIntent = null;

    // Current shared preferences.
    protected SharedPreferences mSharedPreferences = null;

    /**
     * Defines what type of plugin this is.
     * 
     * @return is sandbox plugin
     */
    protected abstract boolean isSandboxPlugin();

    /**
     * Extenders of PluginService must implement this. Called by
     * ServiceConnection.onServiceConnected.
     * 
     * @param className
     * @param service
     */
    protected abstract void onServiceConnectedExtended(ComponentName className, IBinder service);

    /**
     * Extenders of PluginService must implement this. Called by
     * ServiceConnection.onServiceDisconnected.
     * 
     * @param className
     */
    protected abstract void onServiceDisconnectedExtended(ComponentName className);

    /**
     * Extenders of PluginService must implement this. Called by
     * OnSharedPreferenceChangeListener.onSharedPreferenceChanged.
     * 
     * @param pref
     * @param key
     */
    protected abstract void onSharedPreferenceChangedExtended(SharedPreferences pref, String key);

    /**
     * Extenders of PluginService must implement this. Called by
     * OnSharedPreferenceChangeListener.onSharedPreferenceChanged.
     */
    protected abstract void startWork();

    /**
     * Extenders of PluginService must implement this. Called by
     * OnSharedPreferenceChangeListener.onSharedPreferenceChanged.
     */
    protected abstract void stopWork();

    /**
     * LiveView callback interface method.
     * 
     * Will start plugin.
     */
    protected abstract void startPlugin();

    /**
     * LiveView callback interface method.
     * 
     * Will stop plugin.
     */
    protected abstract void stopPlugin();

    /**
     * LiveView callback interface method.
     * 
     * Button has been pressed on the LiveView device.
     */
    protected abstract void button(String buttonType, boolean doublepress, boolean longpress);

    /**
     * LiveView callback interface method.
     * 
     * Gives the display capabilites of the LiveView device.
     */
    protected abstract void displayCaps(int displayWidthPx, int displayHeigthPx);

    /**
     * LiveView callback interface method.
     * 
     * Called when thrown out by framework.
     */
    protected abstract void onUnregistered() throws RemoteException;

    /**
     * LiveView callback interface method.
     * 
     * Open in phone function called from the LiveView device.
     */
    protected abstract void openInPhone(String openInPhoneAction);

    /**
     * LiveView callback interface method.
     * 
     * Gives an indication what the current state of the LiveView device screen.
     * 0 = off, 1 = on
     */
    protected abstract void screenMode(int mode);

    /**
     * LiveView callback interface method.
     */
    private class LiveViewCallback extends IPluginServiceCallbackV1.Stub {

        Handler mCallbackHandler = new Handler();

        @Override
        public void startPlugin() throws RemoteException {
            mCallbackHandler.post(new Runnable() {
                public void run() {
                    AbstractPluginService.this.startPlugin();
                }
            });
        }

        @Override
        public void stopPlugin() throws RemoteException {
            mCallbackHandler.post(new Runnable() {
                public void run() {
                    AbstractPluginService.this.stopPlugin();
                }
            });
        }

        @Override
        public String getPluginName() throws RemoteException {
            return mPluginName;
        }

        @Override
        public void button(final String buttonType, final boolean doublepress,
                final boolean longpress) throws RemoteException {
            mCallbackHandler.post(new Runnable() {
                public void run() {
                    AbstractPluginService.this.button(buttonType, doublepress, longpress);
                }
            });
        }

        @Override
        public void displayCaps(final int displayWidthPx, final int displayHeigthPx)
                throws RemoteException {
            mCallbackHandler.post(new Runnable() {
                public void run() {
                    AbstractPluginService.this.displayCaps(displayWidthPx, displayHeigthPx);
                }
            });
        }

        @Override
        public void onUnregistered() throws RemoteException {
            AbstractPluginService.this.onUnregistered();
        }

        @Override
        public void openInPhone(final String openInPhoneAction) throws RemoteException {
            mCallbackHandler.post(new Runnable() {
                public void run() {
                    AbstractPluginService.this.openInPhone(openInPhoneAction);
                }
            });
        }

        @Override
        public void screenMode(final int mode) throws RemoteException {
            mCallbackHandler.post(new Runnable() {
                public void run() {
                    AbstractPluginService.this.screenMode(mode);
                }
            });
        }

    }

    /**
     * Check if service is already running.
     * 
     * @return running?
     */
    public static boolean isAlreadyRunning() {
        return alreadyRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(PluginConstants.LOG_TAG, "Enter AbstractPluginService.onCreate.");

        // Load menu icon
        int iconId = PluginUtils.getDynamicResourceId(this, "icon", "drawable");
        mMenuIcon = PluginUtils.storeIconToFile(this, getResources(), iconId, TEMPLATE_MENU_ICON);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(PluginConstants.LOG_TAG, "Enter AbstractPluginService.onDestroy.");

        // Unbind from LiveView service
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }

        // No longer a running service
        alreadyRunning = false;
    }

    @Override
    public void onStart(final Intent intent, final int startId) {
        super.onStart(intent, startId);
        Log.d(PluginConstants.LOG_TAG, "Enter AbstractPluginService.onStart.");

        if (isAlreadyRunning()) {
            Log.d(PluginConstants.LOG_TAG, "Already started.");
        } else {
            // Init
            mPluginName = PluginUtils.getDynamicResourceString(this,
                    PluginConstants.RESOURCE_STRING_PLUGIN_NAME);
            mServiceIntent = PluginUtils.getDynamicResourceString(this,
                    PluginConstants.RESOURCE_STRING_INTENT_SERVICE);

            // Get shared preferences
            setPreferences();

            // Register preference listener
            PreferenceManager.getDefaultSharedPreferences(this)
                    .registerOnSharedPreferenceChangeListener(mPrefChangeListener);

            // Bind to LiveView
            connectToLiveView();

            // Singleton
            alreadyRunning = true;
        }
    }

    @Override
    public IBinder onBind(final Intent intent) {
        Log.d(PluginConstants.LOG_TAG, "Enter AbstractPluginService.onBind.");
        return null;
    }

    /**
     * The service connection that is used to bind the plugin to the LiveView
     * service.
     * 
     * When connected to the service, the plugin is registered. When
     * disconnected to the service, the plugin is unregistered.
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(final ComponentName className, IBinder service) {
            Log.d(PluginConstants.LOG_TAG,
                    "Enter AbstractPluginService.ServiceConnection.onServiceConnected.");

            IPluginServiceV1 liveViewService = IPluginServiceV1.Stub.asInterface(service);

            // Init adapter
            mLiveViewAdapter = new LiveViewAdapter(liveViewService);
            LiveViewCallback lvCallback = new LiveViewCallback();

            // Install plugin
            try {
                mPluginId = mLiveViewAdapter.installPlugin(lvCallback, mMenuIcon, mPluginName,
                        isSandboxPlugin(), getPackageName(), mServiceIntent);
            } catch (RemoteException re) {
                Log.e(PluginConstants.LOG_TAG, "Failed to install plugin. Stop self.");
                stopSelf();
            }

            Log.d(PluginConstants.LOG_TAG, "Plugin registered. mPluginId: " + mPluginId
                    + " isSandbox? " + isSandboxPlugin());

            // Call specific plugin implementation
            onServiceConnectedExtended(className, service);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(PluginConstants.LOG_TAG,
                    "Enter AbstractPluginService.ServiceConnection.onServiceDisconnected.");
            stopSelf();
        }

    };

    /**
     * The preference listener. Implement this to get awareness of user
     * preference changes. Is registered in onStart.
     */
    protected OnSharedPreferenceChangeListener mPrefChangeListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            // Update preferences
            setPreferences(prefs);

            if (key.equals(PluginConstants.PREFERENCES_PLUGIN_ENABLED)) {
                boolean pluginEnabled = prefs.getBoolean(
                        PluginConstants.PREFERENCES_PLUGIN_ENABLED, false);
                if (pluginEnabled) {
                    startWork();
                } else {
                    stopWork();
                }

                Log.d(PluginConstants.LOG_TAG, "Preferences changed - enabled: " + pluginEnabled);
            }

            // Call custom plugin implementation
            onSharedPreferenceChangedExtended(prefs, key);
        }

    };

    /**
     * Connects to the LiveView service.
     */
    private void connectToLiveView() {
        boolean result = bindService(new Intent(PluginConstants.LIVEVIEW_SERVICE_BIND_INTENT),
                mServiceConnection, 0);
        if (result) {
            Log.d(PluginConstants.LOG_TAG, "Bound to LiveView.");
        } else {
            Log.d(PluginConstants.LOG_TAG, "No bind.");
            stopSelf();
        }
    }

    /**
     * Fetches and sets the shared preferences.
     */
    private void setPreferences() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    /**
     * Sets the shared preferences.
     */
    private void setPreferences(SharedPreferences prefs) {
        mSharedPreferences = prefs;
    }

}
