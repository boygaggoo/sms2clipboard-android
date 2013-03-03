        	function init() {
        		Settings.getAll( function() {
        			
        			
        			var $toggleButton = $$("#app-toggle-button"),
        				$status = $$("#app-status p");
        			serviceController.isRunning( function(isRunning) {
        				if( isRunning == true ) {
							$toggleButton.removeClass("app-toggle-button-off").addClass("app-toggle-button-on");
        					$status.html( LANG["en"].statusEnabled );
        				}
        					
        			}); 
        				
	        		$toggleButton.on("tap", function() {
	        			var $this = $$(this);
						serviceController.isRunning( function(isRunning) {
							if( isRunning) {
		        				serviceController.stopService( function() {
		        					$status.html( LANG["en"].statusDisabled );
									$this.removeClass("app-toggle-button-on").addClass("app-toggle-button-off");
								});
		        			} else {
								serviceController.startService( function() {
									$status.html( LANG["en"].statusEnabled );
									$this.removeClass("app-toggle-button-off").addClass("app-toggle-button-on");
								});
		        			}
						});
	        			
	        				
	        			
	        		});
	        		
	        		/* Menu */
	        		$$("nav li").on("tap", function() {
	        			$$("nav li").removeClass("current");
	        			var section = $$(this).data("section");
	        			
	        			$$("section.active").removeClass("active");
	        			$$("#" + section).addClass("active");
	        			$$(this).addClass("current");
	        			
	
	        			$$("nav li").removeClass("current");
	        			var section = $$(this).data("section");
	        			$$("section.active").removeClass("active");
	        			$$("#" + section).addClass("active");
	        			$$(this).addClass("current");
	        			
	        		});
        		        		
        		}); 

        	}
        	
        	//$$(document).ready(init);
        	$$(document).on("deviceready", function(){
				init();
        	});
        	
