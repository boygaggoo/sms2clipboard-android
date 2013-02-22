package pl.webartists.sms2clipboard.plugins;

import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceController extends CordovaPlugin {
	
	private String TAG = this.getClass().getName();
	
	@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		
		
        if (action.equals("stopService")) {
            stopService( callbackContext ); 
            return true;
        }
        
        if (action.equals("startService")) {
            startService( callbackContext ); 
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
	
}
