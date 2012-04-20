package fr.spaz.samsungtvnotification;

import android.app.NotificationManager;
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
	}

	private void startService()
	{
		final Intent intent = new Intent(this, SamsungTVNotificationService.class);
		startService(intent);
		mServiceConnected = true;
		invalidateOptionsMenu();
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