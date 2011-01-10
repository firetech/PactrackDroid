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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utils.
 */
public final class PluginUtils {
    
    private PluginUtils() {
        
    }
    
    /**
     * Stores icon to phone file system
     * 
     * @param resources Reference to project resources
     * @param resource Reference to specific resource
     * @param fileName The icon file name
     */
    public static String storeIconToFile(Context ctx, Resources resources, int resource, String fileName) {
        Log.d(PluginConstants.LOG_TAG, "Store icon to file.");
        
        if(resources == null) {
            return "";
        }
        
        Bitmap bitmap = BitmapFactory.decodeStream(resources.openRawResource(resource));
        
        try {
            FileOutputStream fos = ctx.openFileOutput(fileName, Context.MODE_WORLD_READABLE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close(); 
        } 
        catch (IOException e) { 
            Log.e(PluginConstants.LOG_TAG, "Failed to store to device", e);
        }
        
        File iconFile = ctx.getFileStreamPath(fileName);
        Log.d(PluginConstants.LOG_TAG, "Icon stored. " + iconFile.getAbsolutePath());
        
        return iconFile.getAbsolutePath();
    }
    
    /**
     * Rotates and stores image to device
     *  
     * @param bitmap
     * @param degrees
     * @return
     */
    public static void rotateAndSend(LiveViewAdapter liveView, int pluginId, Bitmap bitmap, int degrees) {
        Bitmap newBitmap = null;
        try
        {
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch(Exception e) {
            Log.e(PluginConstants.LOG_TAG, "Failed to rotate bitmap.", e);
            return;
        }
        
        sendScaledImage(liveView, pluginId, newBitmap);
    }
    
    public static void sendTextBitmap(LiveViewAdapter liveView, int pluginId, String text) {
        sendTextBitmap(liveView, pluginId, text, 64, 15);
    }
    
    /**
     * Stores text to an image on file.
     * 
     * @param liveView Reference to LiveView connection
     * @param pluginId Id of the plugin
     * @param text The text string
     * @param bitmapSizeX Bitmap size X
     * @param fontSize Font size
     * @return Absolute path to file
     */
    public static void sendTextBitmap(LiveViewAdapter liveView, int pluginId, String text, int bitmapSizeX, int fontSize) {
        // Empty bitmap and link the canvas to it
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(bitmapSizeX, fontSize, Bitmap.Config.RGB_565);
        }
        catch(IllegalArgumentException  e) {
            return;
        }
        
        Canvas canvas = new Canvas(bitmap);

        // Set the text properties in the canvas
        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(fontSize);
        textPaint.setColor(Color.WHITE);

        // Create the text layout and draw it to the canvas
        Layout textLayout = new StaticLayout(text, textPaint, bitmapSizeX, Layout.Alignment.ALIGN_CENTER, 1, 1, false);
        textLayout.draw(canvas);
        
        try
        { 
            liveView.sendImageAsBitmap(pluginId, centerX(bitmap), centerY(bitmap), bitmap);
        } catch(Exception e) {
            Log.d(PluginConstants.LOG_TAG, "Failed to send bitmap", e);
        }
    }
    
    /**
     * Gets resource id dynamically
     * 
     * @param context
     * @param resourceName
     * @param resourceType
     * @return
     */
    public static int getDynamicResourceId(Context context, String resourceName, String resourceType) {
        return context.getResources().getIdentifier(resourceName, resourceType, context.getPackageName());
    }
    
    /**
     * Gets resource string dynamically
     * 
     * @param context
     * @param resourceName
     * @return
     */
    public static String getDynamicResourceString(Context context, String resourceName) {
        int resourceId = getDynamicResourceId(context, resourceName, "string");
        return context.getString(resourceId);
    }
    
    /**
     * Sends an image to LiveView and puts it in the middle of the screen
     * 
     * @param liveView
     * @param pluginId
     * @param bitmap
     * @param path
     */
    public static void sendScaledImage(LiveViewAdapter liveView, int pluginId, Bitmap bitmap) {
        try {
            if(liveView != null) {
                liveView.sendImageAsBitmap(pluginId, centerX(bitmap), centerY(bitmap), bitmap);
            }
        } catch(Exception e) {
            Log.e(PluginConstants.LOG_TAG, "Failed to send image.", e);
        }
    }
    
    /**
     * Get centered X axle
     * 
     * @param bitmap
     * @return
     */
    private static int centerX(Bitmap bitmap) {
        return (PluginConstants.LIVEVIEW_SCREEN_X/2) - (bitmap.getWidth()/2);
    }
    
    /**
     * Get centered Y axle
     * 
     * @param bitmap
     * @return
     */
    private static int centerY(Bitmap bitmap) {
        return (PluginConstants.LIVEVIEW_SCREEN_Y/2) - (bitmap.getHeight()/2);
    }

}