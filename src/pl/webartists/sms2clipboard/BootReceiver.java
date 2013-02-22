package pl.webartists.sms2clipboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	
	private String TAG = this.getClass().getName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG, "Device booted. Should we start sms2clipboard service?" );
		try {
	        SharedPreferences settings = context.getSharedPreferences("settings", context.MODE_PRIVATE);
	        if( settings.getBoolean("startOnBoot", true ) ) {
	        	Log.v( TAG, "Starting sms2clipboard service on boot.");
	        	context.startService( new Intent(".Sms2ClipboardService"));
	        }			
		} catch( Exception exception ) {
			Log.e( TAG, "Failed to start sms2clipboard service on boot.");
		}
	}
}
