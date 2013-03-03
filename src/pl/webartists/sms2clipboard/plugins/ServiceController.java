package pl.webartists.sms2clipboard.plugins;

import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.webartists.sms2clipboard.Sms2ClipboardService;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class ServiceController extends CordovaPlugin {
	
	private String TAG = this.getClass().getName();
	
	
	@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		Context context = cordova.getActivity().getApplicationContext();
		
        if (action.equals("stopService")) {
            stopService( callbackContext );
            return true;
        }
        
        if (action.equals("startService")) {
            startService( callbackContext ); 
            return true;
        }
        
        if (action.equals("isRunning")) {
            isRunning( callbackContext ); 
            return true;
        }

        return false;
    }
	
	private void startService( CallbackContext callbackContext ) {
		try {
			Context context = cordova.getActivity().getApplicationContext();
			context.startService( new Intent(".Sms2ClipboardService") );
			callbackContext.success();	
		} catch( Exception exception ) {
			Log.e(TAG, "exception", exception);
			callbackContext.error( exception.getMessage() );
		}
	}
	
	private void stopService( CallbackContext callbackContext ) {
		try {
			Context context = cordova.getActivity().getApplicationContext();
			context.stopService( new Intent(".Sms2ClipboardService") );
			callbackContext.success();	
		} catch( Exception exception ) {
			Log.e(TAG, "exception", exception);
			callbackContext.error( exception.getMessage() );
		}
	}
	
	private void isRunning(CallbackContext callbackContext) {
		try {
			Context context = cordova.getActivity().getApplicationContext();
			ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		    for (RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
		        if (Sms2ClipboardService.class.getName().equals(service.service.getClassName())) {
		        	Log.i(TAG, "Service is running....");
		        	callbackContext.success(new JSONObject().put("serviceRunning", true));
		        	return;
		        }
		    }
		    callbackContext.success(new JSONObject().put("serviceRunning", false));			
		} catch( Exception exception ) {
			Log.e(TAG, "exception", exception);
			callbackContext.error( exception.getMessage() );
		}
	}
	
	
}
