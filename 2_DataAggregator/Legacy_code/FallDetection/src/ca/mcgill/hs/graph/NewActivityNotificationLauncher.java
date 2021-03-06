/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.graph;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import falldetection.analysis.fall.R;
import ca.mcgill.hs.prefs.PreferenceFactory;
import ca.mcgill.hs.util.Log;

/**
 * This Service launches a notification for the HSAndroid application. When the
 * classifier wants to collect some information about the user's activity
 * between two timestamps, it first starts by setting the timestamps START and
 * END, and then starts this service.
 * 
 * @author Jordan Frank, Jonathan Pitre
 */
public class NewActivityNotificationLauncher extends Service {

	// These are the two timestamps used by this service. They have to be set
	// beforehand.
	private static long start = -1;
	private static long end = -1;
	private static Float[] magValues = null;
	private static int[] magActivities = null;

	// Notification ID, randomly chosen
	public static final int NOTIFICATION_ID = 455925;

	/**
	 * This method sets the END timestamp. Both the START and END timestamps
	 * have to be set for this service to start properly.
	 * 
	 * @param timestamp
	 *            The END timestamp.
	 */
	public static void setEndTimestamp(final long timestamp) {
		end = timestamp;
	}

	/**
	 * This method sets the magValues.
	 * 
	 * @param data
	 *            An array of floats for the magnitude values.
	 */
	public static void setMagValues(final Float[] data, final int[] activities) {
		magValues = data;
		magActivities = activities;
	}

	/**
	 * This method sets the START timestamp. Both the START and END timestamps
	 * have to be set for this service to start properly.
	 * 
	 * @param timestamp
	 *            The START timestamp.
	 */
	public static void setStartTimestamp(final long timestamp) {
		start = timestamp;
	}

	// Icon used for the notification.
	private int icon;

	// String used for the notification ticker.
	private String tickerText;

	// String used for the notification title.
	private String contentTitle;

	// String used for the notification text.
	private String contentText;

	@Override
	// Unused
	public IBinder onBind(final Intent arg0) {
		return null;
	}

	@Override
	public void onDestroy() {
		start = -1;
		end = -1;
	}

	@Override
	public void onStart(final Intent intent, final int startId) {
		final SharedPreferences prefs = PreferenceFactory
				.getSharedPreferences(this);
		/*
		 * If START and END haven't been set previously using setEndTimeStamp
		 * and setStartTimestamp, this method calls this Service's onDestroy()
		 * method.
		 */
		if (start == -1 || end == -1 || start == end || magValues == null
				|| magActivities == null
				|| !prefs.getBoolean("notificationEnablePref", true)) {
			Log.e("NotificationLauncher",
					"Could not start Service: one or more required "
							+ "values have not been previously set.");
			if (magValues == null) {
				Log.e("NotificationLauncher", "magValues was null;");
			}
			if (magActivities == null) {
				Log.e("NotificationLauncher", "magActivities was null;");
			}
			if (start == -1) {
				Log.e("NotificationLauncher", "start was not set;");
			}
			if (end == -1) {
				Log.e("NotificationLauncher", "end was not set;");
			}
			if (start == end) {
				Log.e("NotificationLauncher",
						"The start and end timestamps were the same;");
			}
			onDestroy();
		} else {
			// Set up MagnitudeGraph
			MagnitudeGraph.setValues(magValues, magActivities);
			MagnitudeGraph.setStartTimestamp(start);
			MagnitudeGraph.setEndTimestamp(end);

			// Create instance variables
			icon = R.drawable.notification_icon;
			tickerText = getResources().getString(R.string.notification_ticker);
			contentTitle = getResources()
					.getString(R.string.notification_title);
			contentText = getResources().getString(R.string.notification_text);

			// Get a reference to the NotificationManager
			final String ns = Context.NOTIFICATION_SERVICE;
			final NotificationManager nm = (NotificationManager) getSystemService(ns);

			// Cancel previous notification.
			nm.cancel(NOTIFICATION_ID);

			// Instantiate the notification
			final Notification n = new Notification(icon, tickerText, System
					.currentTimeMillis());

			// Set preferences
			if (prefs.getBoolean("notificationSoundPref", true)) {
				n.defaults |= Notification.DEFAULT_SOUND;
			}
			if (prefs.getBoolean("notificationVibratePref", true)) {
				n.defaults |= Notification.DEFAULT_VIBRATE;
			}
			n.flags |= Notification.FLAG_AUTO_CANCEL;

			// Show lights
			n.flags |= Notification.FLAG_SHOW_LIGHTS;
			n.defaults |= Notification.DEFAULT_LIGHTS;
			n.ledARGB = 0xff00ff00;

			// Define expanded message and intent
			final Context context = getApplicationContext();
			final Intent notificationIntent = new Intent(this,
					MagnitudeGraph.class);
			final PendingIntent contentIntent = PendingIntent.getActivity(this,
					0, notificationIntent, 0);
			n.setLatestEventInfo(context, contentTitle, contentText,
					contentIntent);

			// Pass notification to NotificationManager
			nm.notify(NOTIFICATION_ID, n);
			onDestroy();
		}
	}
}
