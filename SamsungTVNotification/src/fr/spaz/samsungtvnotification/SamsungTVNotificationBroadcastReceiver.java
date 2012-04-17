package fr.spaz.samsungtvnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SamsungTVNotificationBroadcastReceiver extends BroadcastReceiver
{

	private static final String TAG = "SamsungTVNotificationBroadcastReceiver";
	private static final String PDUS = "pdus";
	private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

	@Override
	public void onReceive(Context _context, Intent _intent)
	{
		if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(_intent.getAction()))
		{
			Log.d(TAG, "Tel state changed");
			if (TelephonyManager.EXTRA_STATE_RINGING.equals(_intent.getStringExtra(TelephonyManager.EXTRA_STATE)))
			{
				Log.d(TAG, "State: Ringing");
				Log.d(TAG, "Number: " + _intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
			}
			if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(_intent.getStringExtra(TelephonyManager.EXTRA_STATE)))
			{
				Log.d(TAG, "State: Offhook");
			}
			if (TelephonyManager.EXTRA_STATE_IDLE.equals(_intent.getStringExtra(TelephonyManager.EXTRA_STATE)))
			{
				Log.d(TAG, "State: idle");
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
			}
		}
	}
}
