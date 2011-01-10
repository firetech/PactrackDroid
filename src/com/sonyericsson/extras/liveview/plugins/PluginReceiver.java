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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receives broadcast intents from LiveView service.
 */
public class PluginReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String command = intent.getExtras().getString(PluginConstants.BROADCAST_COMMAND);
		Log.d(PluginConstants.LOG_TAG, "Received command: " + command);
		
		if(command == null) {
			return;
		}
		
		if(command.contentEquals(PluginConstants.BROADCAST_COMMAND_PREFERENCES)) {
			String pluginName = intent.getExtras().getString(PluginConstants.BROADCAST_COMMAND_PLUGIN_NAME);
			String myPluginName = PluginUtils.getDynamicResourceString(context, PluginConstants.RESOURCE_STRING_PLUGIN_NAME);

			if(pluginName != null && pluginName.contentEquals(myPluginName)) {
				String intentString = PluginUtils.getDynamicResourceString(context, PluginConstants.RESOURCE_STRING_INTENT_PREFS);
				Log.d(PluginConstants.LOG_TAG, "Starting preferences! Intent: " + intentString);
				
				Intent prefsIntent = new Intent(intentString);
				prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(prefsIntent);
			}
		}
		else if(command.contentEquals(PluginConstants.BROADCAST_COMMAND_START)) {
			if(AbstractPluginService.isAlreadyRunning()) {
			    Log.d(PluginConstants.LOG_TAG, "Service is already running.");
			} else {
				String serviceIntent = PluginUtils.getDynamicResourceString(context, PluginConstants.RESOURCE_STRING_INTENT_SERVICE);
				Log.d(PluginConstants.LOG_TAG, "Starting service! Intent: " + serviceIntent);
				
				context.startService(new Intent(serviceIntent));
			}
		}
		
	}
	
}