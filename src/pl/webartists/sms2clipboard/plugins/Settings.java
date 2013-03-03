package pl.webartists.sms2clipboard.plugins;

import java.util.Iterator;
import java.util.Map;

import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class Settings extends CordovaPlugin {
	
	private String TAG = this.getClass().getName();
	SharedPreferences settings;
	
	@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		Context context = cordova.getActivity().getApplicationContext();
		settings = context.getSharedPreferences("settings", context.MODE_PRIVATE);
		Log.i(TAG, action);
        if (action.equals("getAll")) {
            getAll(callbackContext);
            return true;
        }
        
        if (action.equals("setAll")) {
        	Log.i(TAG, args.getJSONObject(0).toString());
            setAll(callbackContext, args.getJSONObject(0));
            return true;
        }

        return false;
    }
	
	private void getAll( CallbackContext callbackContext ) {
		Log.d(TAG, this.toString());
		try {
			JSONObject settingsJson = new JSONObject(settings.getAll());
			Log.i( TAG, settingsJson.toString() );
			callbackContext.success(settingsJson);
		} catch( Exception exception ) {
			Log.e(TAG, "exception", exception);
			callbackContext.error( exception.getMessage() );
		}
	}
	
	private void setAll( CallbackContext callbackContext, JSONObject settingsJson ) {
		Log.i(TAG, settingsJson.toString());
		try {
			Editor settingsEditor = settings.edit();
			Iterator<?> iterator = settingsJson.keys();
						
			while( iterator.hasNext() ) {
				String key = (String)iterator.next();
				Object value = settingsJson.get(key);
				Log.i(TAG , "Settings update: " + key + " = " + value.toString());
				if( value instanceof Boolean) {
					settingsEditor.putBoolean(key, (Boolean)value);
				}
				
				if( value instanceof String) {
					settingsEditor.putString(key, (String)value);
				}
				
				if( value instanceof Integer) {
					settingsEditor.putInt(key, (Integer)value);
				}

			}
			settingsEditor.commit();
			callbackContext.success(settingsJson);
		} catch( Exception exception ) {
			Log.e(TAG, "exception", exception);
			callbackContext.error( exception.getMessage() );
		}
		
	}
	
	
	
}
