package com.bitsetters.android.passwordsafe.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.bitsetters.android.passwordsafe.FrontDoor;
import com.bitsetters.android.passwordsafe.R;

public class ServiceNotification {

	private static final int NOTIFICATION_ID = 1;
	
	/**
	 * @param context
	 * @param force overwrites preferences, if true updates notification regardless the preferences.
	 */
	/*
	public static void updateNotification(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		//if (prefs.getBoolean(PreferenceActivity.PREFS_SHOW_NOTIFICATION, false)) {
			if () {
				setNotification(context);
			} else {
				clearNotification(context);
			}
		
		//} else {
			
		//}

	}
	*/
	
	public static void setNotification(Context context) {

		// look up the notification manager service
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
	
		String text = "Logged in";

		Notification notification = new Notification(
				R.drawable.passicon, null, System
						.currentTimeMillis());
		notification.flags = Notification.FLAG_ONGOING_EVENT;

		// TODO: Launch a new Activity with a big button to log out.
		Intent intent = new Intent(context, FrontDoor.class);
		
		
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setAction(Intent.ACTION_MAIN);
		PendingIntent pi = PendingIntent.getActivity(context, 0,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);				
		// Set the info for the views that show in the notification
		// panel.
		notification.setLatestEventInfo(context, context
				.getString(R.string.app_name), text, pi);
		
		nm.notify(NOTIFICATION_ID, notification);
	}
	
	public static void clearNotification(Context context) {

		// look up the notification manager service
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(NOTIFICATION_ID);
	}
}
