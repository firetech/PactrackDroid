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

/**
 * Plugin common constants.
 */
public final class PluginConstants {

    private PluginConstants() {}

    // Broadcast receiver constants
    public static final String BROADCAST_COMMAND = "CMD";
    public static final String BROADCAST_COMMAND_PREFERENCES = "preferences";
    public static final String BROADCAST_COMMAND_START = "start";
    public static final String BROADCAST_COMMAND_PLUGIN_NAME = "pluginName";

    // Resource string constants
    public static final String RESOURCE_STRING_PLUGIN_NAME = "pluginname";
    public static final String RESOURCE_STRING_INTENT_SERVICE = "intent_service";
    public static final String RESOURCE_STRING_INTENT_PREFS = "intent_preferences";

    // Preference constants
    public static final String PREFERENCES_PLUGIN_ENABLED = "pluginEnabled";

    // LiveView Service bind intent
    public static final String LIVEVIEW_SERVICE_BIND_INTENT = "com.sonyericsson.extras.liveview.PLUGIN_SERVICE_V1";

    // Log tag
    public static final String LOG_TAG = "<PactrackDroid> LiveViewPlugin";

    // LiveView screen sizes
    public static final int LIVEVIEW_SCREEN_X = 128;
    public static final int LIVEVIEW_SCREEN_Y = 128;
}