var Settings = function() {

	var settingsLoaded = false;
	var settingsMap = null;
	
	function errorHandler( error ) {
		console.error( "Settings Plugin JS error", error  );
	}

	return {
		getAll: function( callback ) {
			
			function success( response ) {
				settingsMap = response;
				settingsLoaded = true;
				if( typeof callback == "function" ) {
					callback.call(window, settingsMap);
				} 
			};
		
			cordova.exec( success, errorHandler, "Settings", "getAll", [] );
		},
		
		get: function( key ) {
			if( settingsLoaded && typeof settingsMap[key] !== "undefined" ) {
				return settingsMap[key];
			} else {
				console.error("Settings not laoded or key dosen't exsists.");
			}
		},
		
		set: function( key, value ) {
		
			function success( response ) {

				settingsMap = response;
			}
		
			settingsMap[key] = value;
			cordova.exec( success, errorHandler, "Settings", "setAll", [settingsMap] );
		} 
		
	} 
	
}();