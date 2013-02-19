package pl.webartists.sms2clipboard;


import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.red_folder.phonegap.plugin.backgroundservice.BackgroundService;

import pl.softace.passwordless.net.api.client.ApiClient;
import pl.softace.passwordless.net.api.packet.Packet;
import pl.softace.passwordless.net.api.packet.PingRequest;
import pl.softace.passwordless.net.api.packet.PingResponse;
import pl.softace.passwordless.net.api.packet.SMSPacket;
import pl.softace.passwordless.net.autodiscovery.IAutoDiscoveryClient;
import pl.softace.passwordless.net.autodiscovery.ServerInstance;
import pl.softace.passwordless.net.autodiscovery.impl.UDPAutoDiscoveryClient;
public class SMSReciverService extends BackgroundService {
	
	private final static String TAG = SMSReciverService.class.getSimpleName();
	private SMSReceiver mSMSreceiver;
    private IntentFilter mIntentFilter;
	private String mHelloTo = "World";
	
	@Override
	public void onCreate() {
		super.onCreate();
		mSMSreceiver = new SMSReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(mSMSreceiver, mIntentFilter);
	}

	@Override
	protected JSONObject doWork() {
		
		JSONObject result = new JSONObject();
				
		try {
			IAutoDiscoveryClient autoDiscoveryClient = new UDPAutoDiscoveryClient(0);
			List<ServerInstance> servers = autoDiscoveryClient.findServer();
			Log.i(TAG, "Found servers: " + servers.size());
			for (ServerInstance serverInstance : servers) {
				ApiClient apiClient = new ApiClient(serverInstance.getIp(), 8080);
				apiClient.connect();

				PingRequest ping = new PingRequest();
				ping.setText("ping");
				Packet pingResponse = apiClient.send(ping);
										
				if (pingResponse != null && ((PingResponse) pingResponse).getStatus().equals( pl.softace.passwordless.net.api.packet.enums.Status.OK)) {
					SMSPacket smsPacket = new SMSPacket();
					smsPacket.setId(10);
					smsPacket.setText("sms text");
					Packet smsConfirmation = apiClient.send(smsPacket);

				}

				apiClient.disconnect();
			}
		} catch (Exception e) {
			Log.e(TAG , e.getMessage());
		}
		
		
		return result;	
	}

	@Override
	protected JSONObject getConfig() {
		JSONObject result = new JSONObject();
		
		try {
			result.put("HelloTo", this.mHelloTo);
		} catch (JSONException e) {
		}
		
		return result;
	}

	@Override
	protected void setConfig(JSONObject config) {
		try {
			if (config.has("HelloTo"))
				this.mHelloTo = config.getString("HelloTo");
		} catch (JSONException e) {
		}
		
	}     

	@Override
	protected JSONObject initialiseLatestResult() {
		return null;
	}

	@Override
	protected void onTimerEnabled() {
		
	}

	@Override
	protected void onTimerDisabled() {
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mSMSreceiver);
	}
	
	private class messageRecivedTask extends AsyncTask<SMSPacket, Integer, Boolean> {
		protected Boolean doInBackground(SMSPacket... smsPackets) {
			Log.i(TAG, "Task do in background..........");
			try {
				IAutoDiscoveryClient autoDiscoveryClient = new UDPAutoDiscoveryClient(0);
				List<ServerInstance> servers = autoDiscoveryClient.findServer();
				int count = smsPackets.length;
				Log.i(TAG, "Found servers: " + servers.size());
				for (int i = 0; i < count; i++) {
					for (ServerInstance serverInstance : servers) {
						SMSPacket smsPacket = smsPackets[i];
						Log.i(TAG, "Send to server: " + serverInstance.getIp());
						ApiClient apiClient = new ApiClient(serverInstance.getIp(), 8080);
						apiClient.connect();
						PingRequest ping = new PingRequest();
						ping.setText("ping");
						Packet pingResponse = apiClient.send(ping);
						if (pingResponse != null && ((PingResponse) pingResponse).getStatus().equals(pl.softace.passwordless.net.api.packet.enums.Status.OK)) {							
							Packet smsConfirmation = apiClient.send(smsPacket);
						}
						apiClient.disconnect();						
					}
				}
				
				
				
				
				
					
				
			} catch (Exception e) {
				Log.e(TAG, "exception", e);
				Log.e(TAG, e.getMessage());
				
			}	
	         return true;
	     }

	     protected void onPostExecute(Boolean result) {
	         
	     }
	}
	
	private class SMSReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				Bundle extras = intent.getExtras();
		        if ( extras != null )
		        {
		            Object[] smsextras = (Object[]) extras.get( "pdus" );

		            for ( int i = 0; i < smsextras.length; i++ )
		            {
		                SmsMessage smsmsg = SmsMessage.createFromPdu((byte[])smsextras[i]);
		                String strMsgBody = smsmsg.getMessageBody().toString();
		                String strMsgSrc = smsmsg.getOriginatingAddress();
		                Log.i(TAG, strMsgSrc + ":" + strMsgBody);
		                SMSPacket sms = new SMSPacket();
		                sms.setSource(strMsgSrc);
		                sms.setText(strMsgBody);
		                sms.setTimestamp( smsmsg.getTimestampMillis() );
		                new messageRecivedTask().execute( sms );
		            }

		        }
			} catch( Exception e ) {
				Log.e(TAG, e.getMessage());
			}
		}

	}
}
