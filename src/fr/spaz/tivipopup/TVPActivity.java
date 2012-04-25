package fr.spaz.tivipopup;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import fr.spaz.tivipopup.TVPService.ServiceBinder;

public class TVPActivity extends SherlockFragmentActivity
{

	private static final String TAG = "SamsungTVNotificationActivity";

	private boolean mServiceConnected;
	private ServiceConn mServiceConnection;
	private ServiceBinder mUPnPService;

	private TVPListFragment mListFragment;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mListFragment = (TVPListFragment) getSupportFragmentManager().findFragmentById(R.id.list_fragment);

		mServiceConnection = new ServiceConn();

		startService();
		mServiceConnected = true;
		// bindService();

	}

	@Override
	protected void onResume()
	{
		if (mServiceConnected)
		{
			bindService();
		}
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		if (mServiceConnected)
		{
			unbindService();
		}
		super.onPause();
	}

	private void startService()
	{
		Log.v(TAG, "startService");
		final Intent intent = new Intent(this, TVPService.class);
		startService(intent);
	}

	private void stopService()
	{
		Log.v(TAG, "stopService");
		final Intent intent = new Intent(this, TVPService.class);
		stopService(intent);
	}

	private void bindService()
	{
		Log.v(TAG, "bindService");
		final Intent intent = new Intent(this, TVPService.class);
		bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	private void unbindService()
	{
		Log.v(TAG, "unbindService");
		unbindService(mServiceConnection);
	}

	// @Override
	// public void deviceListChange()
	// {
	// if (null != mAdapter)
	// {
	// mAdapter.uiNotifyDataSetChanged();
	// }
	// }

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		final MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_power :
				if (!mServiceConnected)
				{
					startService();
					bindService();
					mServiceConnected = true;
				}
				else
				{
					unbindService();
					stopService();
					mServiceConnected = false;
				}
				invalidateOptionsMenu();
				break;

			case R.id.menu_refresh :
				if (null != mUPnPService)
				{
					mUPnPService.refresh();
				}
				break;

			default :
				break;
		}
		return true;
	}

	@Override
	public boolean onPreparePanel(int featureId, View view, Menu menu)
	{
		if (mServiceConnected)
		{
			menu.findItem(R.id.menu_power).setIcon(R.drawable.service_on).setTitle("Service started");
		}
		else
		{
			menu.findItem(R.id.menu_power).setIcon(R.drawable.service_off).setTitle("Service stoped");
		}
		return true;
	}

	private class ServiceConn implements ServiceConnection
	{

		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			mUPnPService = (ServiceBinder) service;

			// Register listener
			mUPnPService.registerListener(mListFragment);

			mListFragment.setList(mUPnPService.getDeviceList());

			// get device list
			// final ArrayList<Device<?, ?, ?>> list = mUPnPService.getDeviceList();
			// mAdapter = new DeviceAdapter(SamsungTVNotificationActivity.this, list);
			// setListAdapter(mAdapter);
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
		}
	}
}