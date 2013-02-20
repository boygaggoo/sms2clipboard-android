package pl.webartists.sms2clipboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.softace.sms2clipboard.net.api.client.ApiClient;
import pl.softace.sms2clipboard.net.api.packet.Packet;
import pl.softace.sms2clipboard.net.api.packet.PingRequest;
import pl.softace.sms2clipboard.net.api.packet.PingResponse;
import pl.softace.sms2clipboard.net.api.packet.SMSPacket;
import pl.softace.sms2clipboard.net.autodiscovery.IAutoDiscoveryClient;
import pl.softace.sms2clipboard.net.autodiscovery.ServerInstance;
import pl.softace.sms2clipboard.net.autodiscovery.impl.UDPAutoDiscoveryClient;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;


public class Sms2ClipboardService extends Service {
	
	private SmsReceiver smsReceiver;
	private NetworkChangeReceiver networkChangeReceiver;
	private HashMap<String, ApiClient> apiClients;
	
	private Boolean isDiscovering = false;
	private Boolean isSmsReceiverRegistred = false;
	
	
	
	@Override
	public void onCreate() {

		super.onCreate();
		
		/* Register sms receiver */
		ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if( networkInfo.isConnected() ) {
			registerSmsReceiver();
		}

		/* Register wifi change receiver */
		IntentFilter networkChangeIntentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		networkChangeReceiver = new NetworkChangeReceiver();
		registerReceiver(networkChangeReceiver, networkChangeIntentFilter);

		/* Find servers and init apiClient (AES) */
		apiClients = new HashMap<String, ApiClient>();
		
		if( !isDiscovering ) {
			Log.i("S2C", "Run find server on service start....");
			new FindServersTask().execute();
		}
			
		
	}
	
	private void registerSmsReceiver() {
		if( isSmsReceiverRegistred == false ) {
			isSmsReceiverRegistred = true;
			IntentFilter smsReceriverIntentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
			smsReceiver = new SmsReceiver();
			registerReceiver(smsReceiver, smsReceriverIntentFilter);
		}
	}
	
	private void unregisterSmsReciver() {
		if( isSmsReceiverRegistred )
			unregisterReceiver(smsReceiver);
		isSmsReceiverRegistred = false;
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterSmsReciver();
		unregisterReceiver(networkChangeReceiver);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public class SmsReceiver extends BroadcastReceiver
	{
		
	    public void onReceive(Context context, Intent intent)
	    {
	        Bundle bundle = intent.getExtras();
	        SmsMessage[] messages = null;
	        
	        if (bundle != null)
	        {
	        	
	            Object[] pdus = (Object[])bundle.get("pdus");
	            messages = new SmsMessage[pdus.length];
	            
	            for (int i = 0; i < messages.length; i++)
	            {
	            	SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdus[i]); 
	            	String messageBody = smsMessage.getMessageBody();
	            	String messageSource = smsMessage.getOriginatingAddress();
	            	Long messageTimestamp = smsMessage.getTimestampMillis();
	            	Toast.makeText(context, "SMS from:" + messageSource, Toast.LENGTH_SHORT).show();
	            	
	            	SMSPacket smsPacket = new SMSPacket();
	            	smsPacket.setText(messageBody);
	            	smsPacket.setSource(messageSource);
	            	smsPacket.setTimestamp(messageTimestamp);
	            	new MessageRecivedTask().execute(smsPacket);
	            }
	        }
	    }
	}
		
	public class NetworkChangeReceiver extends BroadcastReceiver
	{
	    public void onReceive(Context context, Intent intent)
	    {
	    	try {
	    		ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
	    		NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    		
	    		if( networkInfo.isConnected() ) {
	    			if( !isDiscovering )
	    				new FindServersTask().execute();
	    			registerSmsReceiver();
	    		} else {
	    			unregisterSmsReciver();
	    		}
	    	} catch( Exception e ) {
	    		Log.e("S2C", "exception", e);
	    	}
	    }
	}
	
	
	
    private class FindServersTask extends AsyncTask<Object, Integer, Void> {
    	@Override
    	protected Void doInBackground(Object... params) {
    		findServers();
    		return null;
    	}
    }
    
    private void findServers() {
    	isDiscovering = true;
		Integer foundServersCount = 0;
		try {
			Log.i("S2C", "Starting discoverty!!!!!!!!!!");
			//Do discovery and make instance for missing apiClients.
			IAutoDiscoveryClient autoDiscoveryClient = new UDPAutoDiscoveryClient(1000);
			List<ServerInstance>foundServers = autoDiscoveryClient.findServer();
			foundServersCount = foundServers.size();
			//We need this list to find apiClient instances that are no longer needed.
			ArrayList<String> foundServersKeys = new ArrayList<String>();
			
			
			for( ServerInstance serverInstance : foundServers ) {
				String hashMapKey = serverInstance.getHostName() + serverInstance.getIp();
				foundServersKeys.add(hashMapKey);
				if( !apiClients.containsKey( hashMapKey )) {
					ApiClient newApiClient = new ApiClient(serverInstance.getIp(), 8080);
					apiClients.put(hashMapKey, newApiClient );
				}
			}
			
			
			//Removing unnecessary apiClient instances...	    			
			Set<String> hashMapKeys = apiClients.keySet();
			for(String hashMapKey : hashMapKeys ) {
				if( !foundServersKeys.contains(hashMapKey) ) {
					hashMapKeys.remove(hashMapKey);
				}
			}
		} catch( Exception exception) {
			Log.e("S2C", "exception", exception);
		}
		Log.i("S2C", "Found servers:" + foundServersCount);
		isDiscovering = false;
    }
    
    
    private class MessageRecivedTask extends AsyncTask<SMSPacket, Integer, Void> {
		protected Void doInBackground(SMSPacket... smsPackets) {
			findServers();
			try { 
				
				int count = smsPackets.length;
				
				for (int i = 0; i < count; i++) {
					SMSPacket smsPacket = (SMSPacket)smsPackets[i];
					for ( ApiClient apiClient : apiClients.values() ) {
						apiClient.connect();
						PingRequest ping = new PingRequest();
						ping.setText("ping");
						Packet pingResponse = apiClient.send(ping);
						if (pingResponse != null && ((PingResponse) pingResponse).getStatus().equals(pl.softace.sms2clipboard.net.api.packet.enums.Status.OK)) {							
							Packet smsConfirmation = apiClient.send(smsPacket);
							Log.i("Mark as read", smsPacket.getSource() + ": " + smsPacket.getText() );
							markMessageRead(smsPacket.getSource(), smsPacket.getText());
						}
						apiClient.disconnect();						
					}
				}
			} catch (Exception e) {
				Log.e("S2C", "exception", e);
				Log.e("S2C", e.getMessage());

			}	
	         return null;
	     }
    }
    
    private void markMessageRead( String messegeSource, String messegeBody) {
        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        try {
        	while (cursor.moveToNext()) {
                if ((cursor.getString(cursor.getColumnIndex("address")).equals(messegeSource)) && (cursor.getInt(cursor.getColumnIndex("read")) == 0)) {
                    if (cursor.getString(cursor.getColumnIndex("body")).startsWith(messegeBody)) {
                        String SmsMessageId = cursor.getString(cursor.getColumnIndex("_id"));
                        ContentValues values = new ContentValues();
                        values.put("read", true);
                        getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id=" + SmsMessageId, null);
                        return;
                    }
                }
            }
        } catch(Exception e) {
        	Log.e("Mark Read", "Error in Read: "+e.toString());
        }
}

}
