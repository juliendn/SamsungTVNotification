package fr.spaz.tivipopup;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class NotificationHelper
{
	@SuppressWarnings("deprecation")
	static Notification getConnectedNotification(Context context, int nbDevice)
	{
		Notification notification = null;
		final int icon = R.drawable.notif_connected;
		final boolean ongoing = true;
		final String title = context.getString(R.string.app_name);
		final String text = context.getString(R.string.notification_text, nbDevice);
		final String ticker = context.getString(R.string.notification_ticker);
		final long when = System.currentTimeMillis();
		final Intent notificationIntent = new Intent();
		final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			notification = new Notification.Builder(context)
					.setSmallIcon(icon)
					.setOngoing(ongoing)
					.setContentTitle(title)
					.setContentText(text)
					.setTicker(ticker)
					.setWhen(when)
					.getNotification();
		}
		else
		{
			notification = new Notification(icon, ticker, when);
			notification.setLatestEventInfo(context, title, text, contentIntent);
			notification.defaults = Notification.DEFAULT_LIGHTS;
			notification.sound = null;
//			notification.defaults = Notification.DEFAULT_ALL;
			notification.flags = Notification.FLAG_ONGOING_EVENT;
		}
		return notification;

	}

	@SuppressWarnings("deprecation")
	static Notification getNotConnectedNotification(Context context)
	{
		Notification notification = null;
		final int icon = R.drawable.notif_disconnected;
		final boolean ongoing = true;
		final String title = context.getString(R.string.app_name);
		final String text = context.getString(R.string.notification_text_empty);
		final String ticker = context.getString(R.string.notification_ticker_empty);
		final long when = System.currentTimeMillis();
		final Intent notificationIntent = new Intent();
		final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			return new Notification.Builder(context)
					.setSmallIcon(icon)
					.setOngoing(ongoing)
					.setContentTitle(title)
					.setContentText(text)
					.setTicker(ticker)
					.setWhen(when)
					.getNotification();
		}
		else
		{
			notification = new Notification(icon, ticker, when);
			notification.setLatestEventInfo(context, title, text, contentIntent);
			notification.defaults = Notification.DEFAULT_LIGHTS|Notification.DEFAULT_VIBRATE;
			notification.sound = null;
//			notification.defaults = Notification.DEFAULT_ALL;
			notification.flags = Notification.FLAG_ONGOING_EVENT;
		}
		return notification;
	}
}
