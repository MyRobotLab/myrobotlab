// FIXME - must be an easy way to include full gui & debug 

//-------RuntimeGUI begin---------

function RuntimeGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.
}

RuntimeGUI.prototype = Object.create(ServiceGUI.prototype);
RuntimeGUI.prototype.constructor = RuntimeGUI;

// --- callbacks begin ---
RuntimeGUI.prototype.getState = function(data) {
};
//--- callbacks end ---

// --- Runtime methods begin ---
RuntimeGUI.prototype.getVersion = function(data) {
	$("#"+this.name+"-version").text(data[0]);
}

RuntimeGUI.prototype.getServiceInfo = function(data) {
	n = this.name;
	var gui = guiMap[this.name];
	
	var possibleServices = data[0].serviceData.serviceInfo;
	for (var property in possibleServices) {
		//alert(property);
		var shortName = property.substring(property.lastIndexOf(".")+1);
		
		//$("<ul class='possibleServices'>").appendTo( "#"+this.name+"-display");
		// <li align='left'>
		$("<a href='#' id='"+shortName+"' class='possibleService'><img class='possibleService' src='/" + shortName + ".png' width='24' height='24' align='left'/> " + shortName + "</a>"+
				"<a target='_blank' class='serviceHelp' href='http://myrobotlab.org/service/"+ shortName +"'><img src='/WebGUI/common/help.png'/></a><br/>" ).appendTo( "#"+this.name+"-display");

		//$("</ul>").appendTo( "#"+this.name+"-display");
		//$("#accordion1").accordion("refresh");
	}
	
	// FIXME - this is working on ALL <A HREFS !!!! - should be targeted by class !
	$(function() {
	    $( ".possibleService" )
	      .button().width("300px")
	      .click(function( event ) {
	        event.preventDefault();
	        $("#dialog-form").dialog("open");
	        $("#service-class").val(event.currentTarget.id);
	        $("#dialog-form").attr("title", "create new "+event.currentTarget.id+" service");
	        //alert($("#service-class").value($(this).attr("id")).attr("id"));
	      });
	  });
	
	$(function() {
	    $( ".serviceHelp" )
	      .button().width("48px");
	  });
	
	var name = $( "#name" );
	var serviceClass = $( "#service-class" );
	
	 $( "#dialog-form" ).dialog({
	      autoOpen: false,
	      height: 300,
	      width: 350,
	      modal: true,
	      buttons: {
	        "create service ": function() {
	          var bValid = true;
	 
	          //bValid = bValid && checkLength( name, "username", 3, 16 );
	          gui.send("createAndStart", new Array(name.val(), serviceClass.val()));
	          if ( bValid ) {
	            $( this ).dialog( "close" );
	          }
	        },
	        cancel: function() {
	          $( this ).dialog( "close" );
	        }
	      },
	      close: function() {
	        //allFields.val( "" ).removeClass( "ui-state-error" );
	      }
	    });
	
};
// --- Runtime methods end ---

// --- overrides begin ---
RuntimeGUI.prototype.attachGUI = function() {
	this.subscribe("publishStatus", "displayStatus"); // TODO DO IN PARENT FRAMEWORK !!!

	this.subscribe("resolveSuccess", "resolveSuccess");
	this.subscribe("resolveError", "resolveError");
	this.subscribe("resolveBegin", "resolveBegin");
	this.subscribe("resolveEnd", "resolveEnd");
	this.subscribe("newArtifactsDownloaded", "newArtifactsDownloaded");

	this.subscribe("registered", "registered");
	this.subscribe("released", "released");
	this.subscribe("failedDependency", "failedDependency");
	this.subscribe("proposedUpdates", "proposedUpdates");
	this.subscribe("getVersion", "getVersion");

	// get the service info for the bound runtime (not necessarily local)
	this.subscribe("getServiceInfo", "getServiceInfo");

	//myService.send(boundServiceName, "broadcastState");
	// FIXME !!! - flakey - do to subscribe not processing before this meathod? Dunno???
	//this.getPossibleServices("all");
	this.send("getVersion");
	this.send("getServiceInfo");
};

RuntimeGUI.prototype.resolveSuccess = function(data) {
	this.info("resolve success for " + data[0]);
};

RuntimeGUI.prototype.resolveBegin = function(data) {
	this.info("begin resolve for " + data[0]);
};

RuntimeGUI.prototype.resolveError = function(data) {
	this.error("could not resolve " + data[0]);
};

RuntimeGUI.prototype.resolveEnd = function(data) {
	this.info("end resolve for " + data[0]);
};

RuntimeGUI.prototype.newArtifactsDownloaded = function(data) {
	this.info("new artifact downloaded for " + data[0]);
};

RuntimeGUI.prototype.detachGUI = function() {
	this.unsubscribe("pulse", "pulse");
	this.unsubscribe("publishState", "getState");
	// broadcast the initial state
};

RuntimeGUI.prototype.init = function() {
	var gui = guiMap[this.name];
	$("#"+this.name+"-noWorky").button().click(function() {
		//var port = $("#"+this.name+"-ports").find(":selected").text();
		  gui.send("noWorky");
	});
	$("#"+this.name+"-saveAll").button().click(function() {
		//var port = $("#"+this.name+"-ports").find(":selected").text();
		  gui.send("saveAll");
	});
	$("#"+this.name+"-releaseAll").button().click(function(event) {
		mrl.sendTo(runtimeName, "releaseAll");
	});
};

RuntimeGUI.prototype.registered = function(data) {
	//alert(data);
	// heavy handed but it works
	this.send("getRegistry");
};
// --- overrides end ---

// --- gui events begin ---

//--- gui events end ---


RuntimeGUI.prototype.getPanel = function() {
	var ret = "<label>version </label> <label class='text ui-widget-content ui-corner-all' value='' name='"+this.name+"' id='"+this.name+"-version'></label>" +
			"<button name='"+this.name+"' id='"+this.name+"-noWorky'>no worky</button>" +
			"<button name='"+this.name+"' id='"+this.name+"-saveAll'>save all</button>" +
			"<button name='"+this.name+"' id='"+this.name+"-releaseAll'>release all</button>" +
			"<div id='"+this.name+"-display'>" + "</div>" +
			"<div id='dialog-form' title='create new service'> " +
			"  <form>" +
			"  <fieldset>" +
			"    <label for='name'>name</label>" +
			"    <input type='hidden' name='service-class' id='service-class' class='text ui-widget-content ui-corner-all' />" +
			"    <input type='text' name='name' id='name' class='text ui-widget-content ui-corner-all' />" +
			"  </fieldset>" +
			"  </form>" +
			"</div>";
	return ret;
}
