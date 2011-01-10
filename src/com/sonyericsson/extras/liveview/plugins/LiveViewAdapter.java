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

import android.graphics.Bitmap;
import android.os.RemoteException;
import android.util.Log;

public class LiveViewAdapter {
    
    // Reference to LiveView application stub
    private IPluginServiceV1 mLiveView = null;
    
    public LiveViewAdapter(IPluginServiceV1 liveView) {
        this.mLiveView = liveView;
    }
    
    public int installPlugin(IPluginServiceCallbackV1 callback, String menuIcon, String pluginName, boolean sandbox, 
            String packageName, String launchIntent) throws RemoteException {
        int pluginId = 0;
        if(mLiveView != null) {
            // Register
            pluginId = mLiveView.register(callback, menuIcon, pluginName, sandbox, packageName);
            Log.d(PluginConstants.LOG_TAG, "Plugin registered. mPluginId: " + pluginId + " isSandbox? " + sandbox);
            
            // Notify installation
            int installedOk = mLiveView.notifyInstalled(launchIntent, pluginName);
            Log.d(PluginConstants.LOG_TAG, "Plugin installation notified.");
            
            if(installedOk >= 0) {
                Log.d(PluginConstants.LOG_TAG, "Registry success!");
            }
            else if(installedOk == -1 ) {
                Log.d(PluginConstants.LOG_TAG, "Already registered!");
            }
        }
        
        return pluginId;
    }
    
    public int register(IPluginServiceCallbackV1 cb, String imageMenu, String pluginName, boolean selectableMenu, String packageName) {
        int result = 0;
        try {
            result = mLiveView.register(cb, imageMenu, pluginName, selectableMenu, packageName);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }
        
        return result;
    }
    
    public void unregister(int id, IPluginServiceCallbackV1 cb) {
        try {
            mLiveView.unregister(id, cb);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }
    }
    
    public void sendAnnounce(int id, String imageAnnounce, String header, String body, long timestamp, String openInPhoneAction) {
        try {
            mLiveView.sendAnnounce(id, imageAnnounce, header, body, timestamp, openInPhoneAction);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }
    }
    
    public void sendImage(int id, int x, int y, String image) {
        try {
            mLiveView.sendImage(id, x, y, image);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }
    }

    public void sendImageAsBitmap(int id, int x, int y, Bitmap bitmapData) {
        try {
            mLiveView.sendImageAsBitmap(id, x, y, bitmapData);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }
    }
    
    public void clearDisplay(int id) {
        try {
            mLiveView.clearDisplay(id);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }
    }
    
    public int notifyInstalled(String launcherIntent, String pluginName) {
        int result = 0;
        try {
            result = mLiveView.notifyInstalled(launcherIntent, pluginName);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }
        
        return result;
    }

    public void ledControl(int id, int rgb565, int delayTime, int onTime) {
        try {
            mLiveView.ledControl(id, rgb565, delayTime, onTime);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }
    }

    public void vibrateControl(int id, int delayTime, int onTime) {
        try {
            mLiveView.vibrateControl(id, delayTime, onTime);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }
    }

    public void sendImageAsBitmapByteArray(int id, int x, int y, byte[] bitmapByteArray) {
        try {
            mLiveView.sendImageAsBitmapByteArray(id, x, y, bitmapByteArray);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }        
    }

    public void screenOff(int id) {
        try {
            mLiveView.screenOff(id);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }
    }

    public void screenDim(int id) {
        try {
            mLiveView.screenDim(id);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }
    }

    public void screenOn(int id) {
        try {
            mLiveView.screenOn(id);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }   
    }
    
    public void screenOnAuto(int id) {
        try {
            mLiveView.screenOnAuto(id);
        } catch(RemoteException re) {
            Log.e(PluginConstants.LOG_TAG, "Unexpected remote exception.");
        }   
    }
    
}
