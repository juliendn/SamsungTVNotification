package fr.spaz.samsungtvnotification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class SamsungTVNotificationActivity extends SherlockFragmentActivity
{

	private static final int NOTIF_ID = 0;
	private boolean mServiceConnected;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mServiceConnected = false;

		final ActionBar actionBar = getSupportActionBar();

		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		actionBar.addTab(actionBar.newTab().setText("Tab1"));
		actionBar.addTab(actionBar.newTab().setText("Tab2"));

		startService();

	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_search :
				if (!mServiceConnected)
				{
					startService();
				}
				else
				{
					stopService();
				}
				break;

			default :
				break;
		}
		return true;
	}

	private void stopService()
	{
		final Intent intent = new Intent(this, SamsungTVNotificationService.class);
		stopService(intent);
		mServiceConnected = false;
		invalidateOptionsMenu();

		final NotificationManager notifMnger = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notifMnger.cancel(NOTIF_ID);
	}

	private void startService()
	{
		final Intent intent = new Intent(this, SamsungTVNotificationService.class);
		startService(intent);
		mServiceConnected = true;
		invalidateOptionsMenu();

		showNotification();
	}

	private void showNotification()
	{
		final NotificationManager notifMnger = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		final Intent intent = new Intent(this, SamsungTVNotificationActivity.class);
		final PendingIntent pendingIntent = PendingIntent.getActivity(this, NOTIF_ID, intent, Intent.FLAG_ACTIVITY_NEW_TASK);

		Notification notification = new Notification.Builder(this).setSmallIcon(R.drawable.notif_connected).setOngoing(true).setContentTitle(getString(R.string.notification_title)).setContentText(getString(R.string.notification_text)).setTicker(getString(R.string.notification_ticker)).setWhen(0l).setContentIntent(pendingIntent).getNotification();
		notifMnger.notify(NOTIF_ID, notification);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		final MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onPreparePanel(int featureId, View view, Menu menu)
	{
		if (mServiceConnected)
		{
			menu.findItem(R.id.menu_search).setIcon(R.drawable.service_on).setTitle("Service started");
		}
		else
		{
			menu.findItem(R.id.menu_search).setIcon(R.drawable.service_off).setTitle("Service stoped");
		}
		return true;
	}

}