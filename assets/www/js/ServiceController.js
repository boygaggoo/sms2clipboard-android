var serviceController = function() {
	
	function errorHandler( error ) {
		console.error( "ServiceController JS error", error  );
		navigator.notification.alert("Błąd", function(){}, "Błąd", "OK");
	}
	
	return {
		startService: function( callback ) {
			cordova.exec( callback, errorHandler, "ServiceController", "startService", [] );
		},
		stopService: function( callback ) {
			alert("Plugin - stopService...");
			cordova.exec( callback, errorHandler, "ServiceController", "stopService", [] );
		}
	} 
	
}();