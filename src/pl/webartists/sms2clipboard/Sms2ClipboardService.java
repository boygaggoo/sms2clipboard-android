package pl.webartists.sms2clipboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

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
import android.net.wifi.WifiManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsMessage;
import android.util.Log;

public class Sms2ClipboardService extends Service {
	
	/* Tag for logging */
	private String TAG = this.getClass().getName();
	
	private SmsReceiver smsReceiver;
	private NetworkChangeReceiver networkChangeReceiver;
	private HashMap<String, ApiClient> apiClients;
	
	/* Flags to maintain async tasks */
	private Boolean isDiscovering = false;
	private Boolean isSmsReceiverRegistred = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		try {
			Log.v(TAG, "Starting Sms2Clipboard background service...");
			apiClients = new HashMap<String, ApiClient>();
			
			/* Register sms receiver if wifi is enabled and connected. */
			ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if( networkInfo.isConnected() ) {
				registerSmsReceiver();
			}

			/* Register wifi state change receiver */
			IntentFilter networkChangeIntentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
			networkChangeReceiver = new NetworkChangeReceiver();
			registerReceiver(networkChangeReceiver, networkChangeIntentFilter);
			Log.v(TAG, "WiFi state receiver registred");

			/* Find servers and init apiClient (AES) if we've got wifi connection */
			if( !isDiscovering && networkInfo.isConnected() ) {
				Log.i(TAG, "Looking for servers on service start...");
				new FindServersTask().execute();
			}			
		} catch ( Exception exception ) {
			Log.e(TAG, "Failed to start Sms2Clipboard background service.", exception);
		}

	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterSmsReciver();
		unregisterReceiver(networkChangeReceiver);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	
	
	private void registerSmsReceiver() {
		Log.v(TAG, "Registering sms receiver...");
		try {
			if( !isSmsReceiverRegistred) {
				isSmsReceiverRegistred = true;
				IntentFilter smsReceriverIntentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
				smsReceiver = new SmsReceiver();
				registerReceiver(smsReceiver, smsReceriverIntentFilter);
				Log.v(TAG, "Sms receiver registred.");
			} else {
				Log.v(TAG, "Sms receiver is already registred.");
			}			
		} catch ( Exception exception ) {
			Log.e(TAG, "Failed to register sms receiver.", exception);
		}

	}
	
	private void unregisterSmsReciver() {
		Log.v(TAG, "Unregistering sms receiver...");
		try {
			if( isSmsReceiverRegistred ) {
				unregisterReceiver(smsReceiver);
				isSmsReceiverRegistred = false;
				Log.v(TAG, "Sms receiver unregistered.");
			} else {
				Log.v(TAG, "Sms receiver is already unregistered.");
			}
		} catch( Exception exception ) {
			Log.e(TAG, "Failed to unregister sms receiver.", exception);
		}
		
	}
	
    private void findServers() {
    	isDiscovering = true;
		Integer foundServersCount = 0;
		Log.i(TAG, "Starting servers discovery...");
		try {
			Log.v(TAG, "There is " + apiClients.size() + " apiClient instances cached.");
			//Do discovery and make instance for missing apiClients.
			IAutoDiscoveryClient autoDiscoveryClient = new UDPAutoDiscoveryClient(1000);
			List<ServerInstance>foundServers = autoDiscoveryClient.findServer();
			foundServersCount = foundServers.size();
			Log.i(TAG , "Found " + foundServersCount + " servers.");
			//We need this list to find apiClient instances that are no longer needed.
			ArrayList<String> foundServersKeys = new ArrayList<String>();
			
			for( ServerInstance serverInstance : foundServers ) {
				String hashMapKey = serverInstance.getHostName() + serverInstance.getIp();
				foundServersKeys.add(hashMapKey);
				if( !apiClients.containsKey( hashMapKey )) {
					Log.v(TAG, "New server at " + serverInstance.getIp() + ". Creating apiClient instance...");
					ApiClient newApiClient = new ApiClient(serverInstance.getIp(), 8080);
					apiClients.put(hashMapKey, newApiClient );
				} else {
					Log.v(TAG, "Server at " + serverInstance.getIp() + " has cached apiClient instance.");
				}
			}
			
			
			//Removing unnecessary apiClient instances...
			Log.v(TAG, "Removing unnecessary apiClient instances...");
			Set<String> hashMapKeys = apiClients.keySet();
			for(String hashMapKey : hashMapKeys ) {
				if( !foundServersKeys.contains(hashMapKey) ) {
					hashMapKeys.remove(hashMapKey);
					Log.v(TAG, "Server " + hashMapKey + " deleted from apiClients cache.");
				}
			}
			
			
//			Below is buggy code with ping....
//			
//			Log.v(TAG, "Removing unnecessary apiClient instances...");
//			Set<String> hashMapKeys = apiClients.keySet();
//			for(String hashMapKey : hashMapKeys ) {
//				if( !foundServersKeys.contains(hashMapKey) ) {
//					Log.v("TAG", "Sending ping to lost server:" + hashMapKey);
//					try {
//						ApiClient lostServerApi = apiClients.get(hashMapKey);
//						if( !lostServerApi.isConnected() )
//							lostServerApi.connect();
//						
//						PingRequest ping = new PingRequest();
//						ping.setText("ping");
//						Packet pingResponse = lostServerApi .send(ping);
//						lostServerApi.disconnect();
//						if (pingResponse != null && ((PingResponse) pingResponse).getStatus().equals(pl.softace.sms2clipboard.net.api.packet.enums.Status.OK)) {
//							Log.v(TAG, "Server " + hashMapKey + " responded to ping - don't remove.");
//						} else {
//							Log.v(TAG, "Server " + hashMapKey + " is not respondig.");
//							hashMapKeys.remove(hashMapKey);
//							Log.v(TAG, "Server " + hashMapKey + " deleted from apiClients cache.");
//						}
//						
//					} catch ( Exception exception ) {
//						Log.e(TAG, "Error on sending ping to lost server.", exception);
//					}		
//				}
//			}
		} catch( Exception exception) {
			Log.e("S2C", "exception", exception);
		}
		Log.i("S2C", "Found servers:" + foundServersCount);
		isDiscovering = false;
    }
	
    private void markMessageRead( String messegeSource, String messegeBody) {
        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        Log.v(TAG, "Mark messege as read...");
        try {
        	while (cursor.moveToNext()) {
                if ((cursor.getString(cursor.getColumnIndex("address")).equals(messegeSource)) && (cursor.getInt(cursor.getColumnIndex("read")) == 0)) {
                	Log.v(TAG, "Found unread messege from " + messegeSource + ". Check messege body...");
                    if (cursor.getString(cursor.getColumnIndex("body")).startsWith(messegeBody)) {
                    	
                        String SmsMessageId = cursor.getString(cursor.getColumnIndex("_id"));
                        Log.v(TAG, "Messege found with id:" +  SmsMessageId);
                        ContentValues values = new ContentValues();
                        values.put("read", true);
                        getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id=" + SmsMessageId, null);
                        Log.v(TAG, "Messege marked as read");
                        return;
                    }
                }
            }
        	Log.w(TAG, "Messege to mark as read not found.");
        } catch(Exception exception ) {
        	Log.e(TAG, "Error on mark messege as read: ", exception );
        }
    }

	private class SmsReceiver extends BroadcastReceiver
	{
	    public void onReceive(Context context, Intent intent)
	    {
	    	Log.i(TAG, "New messege received.");
	        Bundle bundle = intent.getExtras();
	        SmsMessage[] recivedMessages = null;
	        
	        if (bundle != null)
	        {
	            Object[] pdus = (Object[])bundle.get("pdus");
	            recivedMessages = new SmsMessage[pdus.length];
	            
	            for (int i = 0; i < recivedMessages.length; i++)
	            {
	            	try {
		            	SmsMessage recivedMessage = SmsMessage.createFromPdu((byte[]) pdus[i]); 
		            	String messageBody = recivedMessage.getMessageBody();
		            	String messageSource = recivedMessage.getOriginatingAddress();
		            	Long messageTimestamp = recivedMessage.getTimestampMillis();
		            	Log.v(TAG, "Messege from " + messageSource + " received on " + messageTimestamp );
		            	SMSPacket smsPacket = new SMSPacket();
		            	smsPacket.setText(messageBody);
		            	smsPacket.setSource(messageSource);
		            	smsPacket.setTimestamp(messageTimestamp);
		            	new MessageRecivedTask().execute(smsPacket);	            		
	            	} catch ( Exception exception ) {
	            		Log.e(TAG, "Failed to parse received message.");
	            	}
	            }
	        }
	    }
	}
		
	public class NetworkChangeReceiver extends BroadcastReceiver
	{
	    public void onReceive(Context context, Intent intent)
	    {
	    	Log.i(TAG, "Wifi connection state changed.");
	    	try {
	    		ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
	    		NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    		Log.v( TAG, "Current Wifi connection state: " + networkInfo.getDetailedState() + "." );
	    		if( networkInfo.isConnected() ) {
	    			Log.i(TAG, "Wifi is connected.");
	    			if( !isDiscovering ) {
	    				Log.i(TAG, "Looking for servers on wifi connected...");
	    				new FindServersTask().execute();
	    			} else {
	    				Log.v(TAG, "Discovery has been already started.");
	    			}

	    			registerSmsReceiver();
	    		} else {
	    			if( isSmsReceiverRegistred )
	    				unregisterSmsReciver();
	    		}
	    	} catch( Exception exception ) {
	    		Log.e(TAG, "Failed to maintain wifi state change.", exception );
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
    
   
    
    private class MessageRecivedTask extends AsyncTask<SMSPacket, Integer, Void> {
		protected Void doInBackground(SMSPacket... smsPackets) {
			Log.v(TAG, "Looking for servers on messege received task...");
			findServers();
			try { 
				for (SMSPacket smsPacket : smsPackets) {
					for ( Entry<String, ApiClient> item: apiClients.entrySet() ) {
						Log.v(TAG, "Trying to send smsPacket to server: " + item.getKey() + "...");
						ApiClient apiClient = item.getValue();
						apiClient.connect();
						PingRequest ping = new PingRequest();
						ping.setText("ping");
						Packet pingResponse = apiClient.send(ping);
						if (pingResponse != null && ((PingResponse) pingResponse).getStatus().equals(pl.softace.sms2clipboard.net.api.packet.enums.Status.OK)) {
							Log.v(TAG, "Server " + item.getKey() + " ping status OK. Send smsPacket.");
							Packet smsConfirmation = apiClient.send(smsPacket);
							Log.v(TAG, "smsPacket sent.");
							markMessageRead(smsPacket.getSource(), smsPacket.getText());
						}
						apiClient.disconnect();						
					}
				}
			} catch (Exception exception ) {
				Log.e(TAG, "Error on sending smsPacket.", exception);
			}	
	         return null;
	     }
    }
    


}
