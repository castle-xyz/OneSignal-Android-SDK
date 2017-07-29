/**
 * Modified MIT License
 *
 * Copyright 2017 OneSignal
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * 1. The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * 2. All copies of substantial portions of the Software may only be used in connection
 * with services provided by OneSignal.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.onesignal;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;

class NotificationChannelManager {
   
   // Can't create a channel with the id 'miscellaneous' as an exception is thrown.
   // Using it results in the notification not being displayed.
   // private static final String DEFAULT_CHANNEL_ID = "miscellaneous"; // NotificationChannel.DEFAULT_CHANNEL_ID;
   
   private static final String DEFAULT_CHANNEL_ID = "fcm_fallback_notification_channel";
   
   static String createNotificationChannel(Context context, JSONObject jsonPayload) {
      OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Build.VERSION.SDK_INT: " + Build.VERSION.SDK_INT);
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
         return DEFAULT_CHANNEL_ID;

      JSONObject payload = jsonPayload;

// To test with additional data
//      JSONObject customJson = null;
//      try {
//         customJson = new JSONObject(jsonPayload.optString("custom"));
//      } catch (JSONException e) {
//         e.printStackTrace();
//      }
//      JSONObject payload = customJson.optJSONObject("a");
   
      NotificationManager notificationManager =
          (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
      
      // TODO: Check for oth_chnl key, if this existing and chanel is already registered use this id
      //         to allow any channels created outside of the SDK.
      
      if (!payload.has("chnl"))
         return createDefaultChannel(notificationManager);
      
      try {
         JSONObject channelPayload = payload.getJSONObject("chnl");
         
         String channel_id = channelPayload.optString("id", DEFAULT_CHANNEL_ID);
         // Ensure we don't try to use the system reserved id
         if (channel_id.equals(NotificationChannel.DEFAULT_CHANNEL_ID))
            channel_id = DEFAULT_CHANNEL_ID;
         
         int importance = channelPayload.optInt("imp", NotificationManager.IMPORTANCE_DEFAULT);
         String channel_name = channelPayload.optString("nm", "Miscellaneous");
         
         NotificationChannel channel = new NotificationChannel(channel_id, channel_name, importance);
         
         if (channelPayload.has("grp")) {
            String group_id = channelPayload.optString("grp");
            CharSequence group_name = channelPayload.optString("grp_nm");
            notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(group_id, group_name));
            channel.setGroup(group_id);
         }
         
         channel.enableLights(channelPayload.optBoolean("lght", true));
         if (channelPayload.has("ledc")) {
            BigInteger ledColor = new BigInteger(channelPayload.optString("ledc"), 16);
            channel.setLightColor(ledColor.intValue());
         }
         
         channel.enableVibration(channelPayload.optBoolean("vib", true));
         if (channelPayload.has("vib_pt")) {
            JSONArray json_vib_array = channelPayload.optJSONArray("vib_pt");
            long[] long_array = new long[json_vib_array.length()];
            for (int i = 0; i < json_vib_array.length(); i++)
               long_array[i] = json_vib_array.optLong(i);
            channel.setVibrationPattern(long_array);
         }
         
         if (channelPayload.has("snd_nm")) {
            // Sound will only play if Importance is set to High or Urgent
            Uri uri = OSUtils.getSoundUri(context, channelPayload.optString("snd_nm", null));
            if (uri!= null)
               channel.setSound(uri, null);
         }
         else if (!channelPayload.optBoolean("snd", true))
            channel.setSound(null, null);
         // Setting sound to null makes it 'None' in the Settings.
         // Otherwise not calling setSound makes it the default notification sound.
         
         channel.setLockscreenVisibility(channelPayload.optInt("lck", Notification.VISIBILITY_PUBLIC));
         channel.enableVibration(channelPayload.optBoolean("lght", true));
         channel.setShowBadge(channelPayload.optBoolean("bdg", true));
         channel.setBypassDnd(channelPayload.optBoolean("bdnd", false));
         
         notificationManager.createNotificationChannel(channel);
         return channel_id;
      } catch (JSONException e) {
         OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Count not create notification channel due to JSON payload error!", e);
      }
      
      return DEFAULT_CHANNEL_ID;
   }
   
   @RequiresApi(api = Build.VERSION_CODES.O)
   private static String createDefaultChannel(NotificationManager notificationManager) {
      NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID,
          "Miscellaneous",
          NotificationManager.IMPORTANCE_DEFAULT);
      
      channel.enableLights(true);
      channel.enableVibration(true);
      notificationManager.createNotificationChannel(channel);
      
      return DEFAULT_CHANNEL_ID;
   }
   
   // TODO: 1. Check JSONObject for a chnl_lst key.
   //       2. Create a set of new channels.
   //       3. Remove any other 'OS_' not defined in this payload
   private static void processChannelList(JSONObject payload) {
   
   }
}