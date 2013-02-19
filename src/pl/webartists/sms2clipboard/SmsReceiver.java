package pl.webartists.sms2clipboard;
import java.util.List;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;
import android.util.Log;
import android.os.AsyncTask;


import pl.softace.sms2clipboard.net.api.client.ApiClient;
import pl.softace.sms2clipboard.net.api.packet.Packet;
import pl.softace.sms2clipboard.net.api.packet.PingRequest;
import pl.softace.sms2clipboard.net.api.packet.PingResponse;
import pl.softace.sms2clipboard.net.api.packet.SMSPacket;
import pl.softace.sms2clipboard.net.autodiscovery.IAutoDiscoveryClient;
import pl.softace.sms2clipboard.net.autodiscovery.ServerInstance;
import pl.softace.sms2clipboard.net.autodiscovery.impl.UDPAutoDiscoveryClient;

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
            	new messageRecivedTask().execute(smsPacket);
            }
        }
    }
    
    private class messageRecivedTask extends AsyncTask<SMSPacket, Integer, Boolean> {
		protected Boolean doInBackground(SMSPacket... smsPackets) {
			Log.i("S2C", "Task do in background..........");
			try {
				IAutoDiscoveryClient autoDiscoveryClient = new UDPAutoDiscoveryClient(0);
				List<ServerInstance> servers = autoDiscoveryClient.findServer();
				int count = smsPackets.length;
				Log.i("S2C", "Found servers: " + servers.size());
				for (int i = 0; i < count; i++) {
					for (ServerInstance serverInstance : servers) {
						SMSPacket smsPacket = smsPackets[i];
						Log.i("S2C", "Send to server: " + serverInstance.getIp());
						ApiClient apiClient = new ApiClient(serverInstance.getIp(), 8080);
						apiClient.connect();
						PingRequest ping = new PingRequest();
						ping.setText("ping");
						Packet pingResponse = apiClient.send(ping);
						if (pingResponse != null && ((PingResponse) pingResponse).getStatus().equals(pl.softace.sms2clipboard.net.api.packet.enums.Status.OK)) {							
							Packet smsConfirmation = apiClient.send(smsPacket);
						}
						apiClient.disconnect();						
					}
				}







			} catch (Exception e) {
				Log.e("S2C", "exception", e);
				Log.e("S2C", e.getMessage());

			}	
	         return true;
	     }
    }

    
}