package fr.spaz.samsungtvnotification;

import java.util.ArrayList;

import org.teleal.cling.model.meta.Device;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import fr.spaz.samsungtvnotification.SamsungTVNotificationService.ServiceBinder;

public class SamsungTVNotificationActivity extends SherlockListActivity implements OnDeviceListChangeListener
{

	private static final String TAG = "SamsungTVNotificationActivity";
	
	private boolean mServiceConnected;
	private ServiceConn mServiceConnection;
	private DeviceAdapter mAdapter;
	private ServiceBinder mUPnPService;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

//		mAdapter = new DeviceAdapter(this);

		// final TextView empty = (TextView)findViewById(android.R.id.empty);
		// empty.setText(R.string.notification_text_empty);

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
		final Intent intent = new Intent(this, SamsungTVNotificationService.class);
		startService(intent);
	}

	private void stopService()
	{
		Log.v(TAG, "stopService");
		final Intent intent = new Intent(this, SamsungTVNotificationService.class);
		stopService(intent);
	}

	private void bindService()
	{
		Log.v(TAG, "bindService");
		final Intent intent = new Intent(this, SamsungTVNotificationService.class);
		bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	private void unbindService()
	{
		Log.v(TAG, "unbindService");
		unbindService(mServiceConnection);
	}

	@Override
	public void deviceListChange()
	{
		if(null!=mAdapter)
		{
			mAdapter.uiNotifyDataSetChanged();
		}
	}

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
				
			case R.id.menu_refresh:
				if(null!=mUPnPService)
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
			mUPnPService.registerListener(SamsungTVNotificationActivity.this);
			
			// get device list
			final ArrayList<Device<?,?,?>> list = mUPnPService.getDeviceList();
			mAdapter = new DeviceAdapter(SamsungTVNotificationActivity.this, list);
			setListAdapter(mAdapter);
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
		}
	}

	public class DeviceAdapter extends BaseAdapter
	{

		private ArrayList<Device<?, ?, ?>> mList;
		private Context mContext;
		private Handler mHandler;
		private Runnable mRunnable;

		public DeviceAdapter(Context context, ArrayList<Device<?, ?, ?>> list)
		{
			mHandler = new Handler();
			mRunnable = new Runnable()
			{
				
				@Override
				public void run()
				{
					notifyDataSetChanged();
				}
			};
			mContext = context;
			mList = list;
		}

		@Override
		public int getCount()
		{
			synchronized (mList)
			{
				return mList.size();	
			}
		}

		@Override
		public Object getItem(int position)
		{
			synchronized (mList)
			{
				return mList.get(position);
			}
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}
		
		public void uiNotifyDataSetChanged()
		{
			mHandler.post(mRunnable);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			if (null == view)
			{
				view = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_2, parent, false);
			}

			Device<?, ?, ?> device = null;

			synchronized (mList)
			{
				device = mList.get(position);
			}
			
			final TextView text1 = (TextView) view.findViewById(android.R.id.text1);
			final TextView text2 = (TextView) view.findViewById(android.R.id.text2);
			text1.setText(device.getDetails().getModelDetails().getModelName());
			text2.setText(device.getDetails().getManufacturerDetails().getManufacturer());
			return view;
		}

	}
}