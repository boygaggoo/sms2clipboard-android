package pl.webartists.sms2clipboard;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;


public class Sms2ClipboardService extends Service {
	
	private SmsReceiver smsReceiver;
	
	@Override
	public void onCreate() {
		Log.i("S2C", "Service created");
		super.onCreate();
		IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		Context context = getApplicationContext();
		smsReceiver = new SmsReceiver();
		context.registerReceiver(smsReceiver, intentFilter);
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Context context = getApplicationContext();
		context.unregisterReceiver(smsReceiver);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
