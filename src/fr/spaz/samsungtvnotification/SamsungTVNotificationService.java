package fr.spaz.samsungtvnotification;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SamsungTVNotificationService extends Service
{

	private static final String TAG = "SamsungTVNotificationService";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.i(TAG, "Start service");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy()
	{
		Log.i(TAG, "Stop service");
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
