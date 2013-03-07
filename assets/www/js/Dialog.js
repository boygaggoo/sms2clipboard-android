var dialog = function() {

	var $dialog = false,
		$title, $content, $buttons;
	
	function create() {
		if( !$dialog ) {
			$dialog = $$("<aside class='dialog'><article><h1></h1><div class='content'></div><footer></footer></article></aside>");
			$title = $dialog.find("h1");
			$content = $dialog.find(".content");
			$buttons = $dialog.find("footer");
			
			$$("body").append($dialog);
		}
		
		$dialog.on("tap", function(){
			$dialog.removeClass("active");
		});
		 
		$dialog.on("webkitTransitionEnd", function(){
			if( !$dialog.hasClass("active") ) {
				$dialog.remove();
				$dialog = false;
			}
		});
		
	}
	
	
	
	return {
		show: function(options) {
			if(!options) options = {};
			var defaults = {
				title: "Confirm",
				content: "Are you sure? <br />Are you sure? <br />",
				buttons: {
					"Cancel": function() {
						$dialog.addClass("hidden");
					}
				}
				
			}
			
			if( !$dialog )
				create();
				
			$title.html( options.title ? options.title : defaults.title );
			$content.html( options.content ? options.content : defaults.content );
			
			setTimeout(function(){
				$dialog.addClass("active");
			},1);
			
		},
		
		updateContent: function(content) {
		
		},
		hide: function() {
			$dialog.removeClass("active");
		}	
	}

}();

dialog.show();