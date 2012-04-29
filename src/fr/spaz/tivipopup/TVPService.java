package fr.spaz.tivipopup;

import java.util.ArrayList;
import java.util.Calendar;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.support.messagebox.AddMessage;
import org.teleal.cling.support.messagebox.model.DateTime;
import org.teleal.cling.support.messagebox.model.Message;
import org.teleal.cling.support.messagebox.model.Message.DisplayType;
import org.teleal.cling.support.messagebox.model.MessageIncomingCall;
import org.teleal.cling.support.messagebox.model.MessageSMS;
import org.teleal.cling.support.messagebox.model.NumberName;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

public class TVPService extends android.app.Service
{

	private enum StateMachine
	{
		STOPPED, OFFLINE, NOTCONNECTED, CONNECTED
	};

	private static final String TAG = "SamsungTVNotificationService";
	private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	private static final int NOTIF_ID = 0;
	@SuppressWarnings("unused")
	private static final int SEARCH_TIME = 10000;

	private UPnPBrowseRegistryListener mListener;
	private UPnPBrowseServiceConnection mServiceConnection;
	private AndroidUpnpService mUpnpService;
	private EventReceiver mReceiver;
	private WifiReceiver mWifiReceiver;

	private OnDeviceListChangeListener mOnDeviceListChangeListener;
	private ArrayList<Device<?, ?, ?>> mList;

	@Override
	public void onCreate()
	{
		Log.i(TAG, "create service");

		mList = new ArrayList<Device<?, ?, ?>>();
		mListener = new UPnPBrowseRegistryListener();
		mServiceConnection = new UPnPBrowseServiceConnection();

		final WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
		Log.i(TAG, "wifi state: " + state);

		mWifiReceiver = new WifiReceiver();
		final IntentFilter wifiFilter = new IntentFilter();
		wifiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		wifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		wifiFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		wifiFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		registerReceiver(mWifiReceiver, wifiFilter);

		final Intent intent = new Intent(this, AndroidUpnpServiceImpl.class);
		bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

		mReceiver = new EventReceiver();
		final IntentFilter filter = new IntentFilter();
		filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		filter.addAction(SMS_RECEIVED);
		registerReceiver(mReceiver, filter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		final NotificationManager notifMnger = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notifMnger.cancel(NOTIF_ID);

		Log.i(TAG, "Stop service");
		if (null != mServiceConnection)
		{
			unbindService(mServiceConnection);
			mServiceConnection = null;
		}
		if (null != mReceiver)
		{
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}
		if (null != mWifiReceiver)
		{
			unregisterReceiver(mWifiReceiver);
			mWifiReceiver = null;
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0)
	{
		return new ServiceBinder();
	}

	private void showNotification(StateMachine state)
	{
		final NotificationManager notifMnger = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		switch (state)
		{
			case STOPPED :
				notifMnger.cancel(NOTIF_ID);
				break;

			case OFFLINE :
				break;

			case CONNECTED :
				notifMnger.notify(NOTIF_ID, NotificationHelper.getConnectedNotification(this, mList.size()));
				break;

			case NOTCONNECTED :
				notifMnger.notify(NOTIF_ID, NotificationHelper.getNotConnectedNotification(this));
				break;

			default :
				break;
		}
	}

	public void searchDevice()
	{
		if (null != mUpnpService)
		{
			Log.i(TAG, "Searching for devices");

		}
	}

	private class UPnPBrowseServiceConnection implements ServiceConnection
	{

		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			mUpnpService = (AndroidUpnpService) service;

			Log.v(TAG, "Notification: NOTCONNECTED");
			showNotification(StateMachine.NOTCONNECTED);

			// Refresh the list with all known devices
			for (Device<?, ?, ?> device : mUpnpService.getRegistry().getDevices())
			{
				mListener.deviceAdded(device);
			}

			// Getting ready for future device advertisements
			Log.v(TAG, "listener registered");
			mUpnpService.getRegistry().addListener(mListener);

			// Search asynchronously for all devices
			mUpnpService.getControlPoint().search();
		}

		@Override
		public void onServiceDisconnected(ComponentName className)
		{
			mUpnpService = null;
		}

	}

	private class EventReceiver extends BroadcastReceiver
	{
		private static final String PDUS = "pdus";

		@Override
		public void onReceive(Context _context, Intent _intent)
		{
			Log.i(TAG, "Event received");
			if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(_intent.getAction()))
			{
				Log.i(TAG, "Tel state changed");
				if (TelephonyManager.EXTRA_STATE_RINGING.equals(_intent.getStringExtra(TelephonyManager.EXTRA_STATE)))
				{
					Log.i(TAG, "State: Ringing");
					final String callNumber = _intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
					Log.i(TAG, "Number: " + callNumber);
					callEvent(callNumber);
				}
				if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(_intent.getStringExtra(TelephonyManager.EXTRA_STATE)))
				{
					Log.i(TAG, "State: Offhook");
				}
				if (TelephonyManager.EXTRA_STATE_IDLE.equals(_intent.getStringExtra(TelephonyManager.EXTRA_STATE)))
				{
					Log.i(TAG, "State: idle");
				}
			}
			if (SMS_RECEIVED.equals(_intent.getAction()))
			{
				Bundle bundle = _intent.getExtras();

				if (bundle != null)
				{
					Object[] pdus = (Object[]) bundle.get(PDUS);
					final SmsMessage[] messages = new SmsMessage[pdus.length];
					final StringBuilder sbuilder = new StringBuilder();
					for (int i = 0; i < pdus.length; i++)
					{
						messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
						sbuilder.append(messages[i].getMessageBody());
					}
					if (messages.length > -1)
					{
						Log.i(TAG, "Message size: " + Integer.toString(pdus.length));
						Log.i(TAG, "Message recieved: " + sbuilder.toString());
						Log.i(TAG, "Message sender: " + messages[0].getDisplayOriginatingAddress());
					}
					smsEvent(messages[0].getDisplayOriginatingAddress(), sbuilder.toString());
				}
			}
		}

		private void callEvent(String callerNumber)
		{
			if (null != mUpnpService && null != mList && mList.size() > 0)
			{
				// time
				final Calendar calendar = Calendar.getInstance();
				final String date = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
				final String hour = String.format("%02d:%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));

				// callee
				final String calleeNumber = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getLine1Number();
				String callee = Build.MODEL;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				{
					final Uri uri = ContactsContract.Profile.CONTENT_URI;
					final Cursor cursor = getContentResolver().query(uri, new String[]{ContactsContract.Profile.DISPLAY_NAME}, null, null, null);
					if (cursor.moveToFirst())
					{
						callee = cursor.getString(cursor.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME));
						Log.i(TAG, "name: " + callee);
					}
					cursor.close();
				}

				// caller
				final Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(callerNumber));
				String caller = null;

				final Cursor cursor = getContentResolver().query(uri, new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);
				if (cursor.moveToFirst())
				{
					caller = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
					Log.i(TAG, "name: " + caller);
				}
				else
				{
					caller = "";
				}
				cursor.close();

				final MessageIncomingCall msg = new MessageIncomingCall(DisplayType.MAXIMUM, new DateTime(date, hour), new NumberName(calleeNumber, callee), new NumberName(callerNumber, caller));

				sendEvent(msg);
			}
		}

		private void smsEvent(String callerNumber, String message)
		{
			if (null != mUpnpService && null != mList && mList.size() > 0)
			{
				// time
				final Calendar calendar = Calendar.getInstance();
				final String date = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
				final String hour = String.format("%02d:%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));

				// callee
				final String calleeNumber = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getLine1Number();
				String callee = Build.MODEL;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				{
					final Uri uri = ContactsContract.Profile.CONTENT_URI;
					final Cursor cursor = getContentResolver().query(uri, new String[]{ContactsContract.Profile.DISPLAY_NAME}, null, null, null);
					if (cursor.moveToFirst())
					{
						callee = cursor.getString(cursor.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME));
						Log.i(TAG, "name: " + callee);
					}
					cursor.close();
				}

				// caller
				final Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(callerNumber));
				String caller = null;

				final Cursor cursor = getContentResolver().query(uri, new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);
				if (cursor.moveToFirst())
				{
					caller = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
					Log.i(TAG, "name: " + caller);
				}
				else
				{
					caller = "";
				}
				cursor.close();

				final MessageSMS msg = new MessageSMS(DisplayType.MAXIMUM, new DateTime(date, hour), new NumberName(calleeNumber, callee), new NumberName(callerNumber, caller), message);

				sendEvent(msg);
			}
		}

		private void sendEvent(Message msg)
		{
			for (Device<?, ?, ?> device : mList)
			{
				Service<?, ?> service = device.findService(new ServiceId("samsung.com", "MessageBoxService"));

				mUpnpService.getControlPoint().execute(new AddMessage(service, msg)
				{

					@SuppressWarnings("rawtypes")
					@Override
					public void success(ActionInvocation invocation)
					{
						Log.i(TAG, "success");
					}

					@SuppressWarnings("rawtypes")
					@Override
					public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg)
					{
						Log.i(TAG, "success");
					}
				});
			}
		}
	}

	public class WifiReceiver extends BroadcastReceiver
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction()))
			{
				NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				if (info.getState().equals(NetworkInfo.State.CONNECTED))
				{

				}
			}
		}
	}

	public class UPnPBrowseRegistryListener extends DefaultRegistryListener
	{

		private static final String TAG = "UpnpBrowseRegistryListener";

		@Override
		public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device)
		{
			deviceAdded(device);
		}

		@Override
		public void remoteDeviceDiscoveryFailed(Registry registry, final RemoteDevice device, final Exception ex)
		{
			deviceRemoved(device);
		}

		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device)
		{
			deviceAdded(device);
		}

		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device)
		{
			deviceRemoved(device);
		}

		@Override
		public void localDeviceAdded(Registry registry, LocalDevice device)
		{
			deviceAdded(device);
		}

		@Override
		public void localDeviceRemoved(Registry registry, LocalDevice device)
		{
			deviceRemoved(device);
		}

		public void deviceAdded(final Device<?, ?, ?> device)
		{
			Log.v(TAG, "Device found. Search for message service");
			if (null != device.findService(new ServiceId("samsung.com", "MessageBoxService")) && device.isFullyHydrated())
			{
				synchronized (mList)
				{
					int position = mList.indexOf(device);
					if (position >= 0)
					{
						// Device already in the list, re-set new value at same position
						mList.remove(device);
						mList.add(position, device);
					}
					else
					{
						mList.add(device);
					}
				}
				Log.i(TAG, "device added: " + device.toString());

				if (null != mOnDeviceListChangeListener)
				{
					mOnDeviceListChangeListener.deviceListChange();
				}

				showNotification(StateMachine.CONNECTED);
			}
		}

		public void deviceRemoved(final Device<?, ?, ?> device)
		{

			if (null != device.findService(new ServiceId("samsung.com", "MessageBoxService")))
			{
				synchronized (mList)
				{
					mList.remove(device);
				}
				Log.i(TAG, "device removed: " + device.toString());
				showNotification(mList.size() > 0 ? StateMachine.CONNECTED : StateMachine.NOTCONNECTED);

			}
		}
	}

	public class ServiceBinder extends Binder
	{

		public void registerListener(OnDeviceListChangeListener listener)
		{
			mOnDeviceListChangeListener = listener;
		}

		public ArrayList<Device<?, ?, ?>> getDeviceList()
		{
			return mList;
		}

		public void refresh()
		{
			if (null != mUpnpService)
			{
				Log.v(TAG, "refresh");
				synchronized (mList)
				{
					mList.clear();
				}
				if (null != mOnDeviceListChangeListener)
				{
					mOnDeviceListChangeListener.deviceListChange();
				}
				mUpnpService.getRegistry().removeAllRemoteDevices();
				mUpnpService.getControlPoint().search();
			}
		}
	}
}
