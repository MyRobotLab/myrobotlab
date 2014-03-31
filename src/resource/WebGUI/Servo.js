function ServoGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.
}

ServoGUI.prototype = Object.create(ServiceGUI.prototype);
ServoGUI.prototype.constructor = ServoGUI;

// --- callbacks begin ---
ServoGUI.prototype.pulse = function(data) {
	$("#"+this.name+"-display").html(data);
};

ServoGUI.prototype.getState = function(data) {
	
};
//--- callbacks end ---

// --- overrides begin ---
ServoGUI.prototype.attachGUI = function() {
	this.subscribe("publishState", "getState");
	// broadcast the initial state
	this.send("broadcastState");
};

ServoGUI.prototype.detachGUI = function() {
	this.unsubscribe("publishState", "getState");
};

ServoGUI.prototype.init = function() {

	$("#"+this.name+"-slider").slider({ 
    	max: 255,
    	slide: function( event, ui ) {
    		var gui = guiMap[$(this).attr("name")];
    		gui.send("moveTo",[ui.value])
    		$("#"+this.name+"-slider-value").val(ui.value);
          }
    });
	
	//$("#" + this.name + "-controller").button().combobox();
	//$("#" + this.name + "-controller").append($('<option></option>').val("arduino").html("arduino option"));
};
// --- overrides end ---

// --- gui events begin ---
ServoGUI.prototype.attach = function(event) {

}

//--- gui events end ---

ServoGUI.prototype.getPanel = function() {
	return "<div>" +
	"controller <select name='"+this.name+"' id='"+this.name+"-controller'><option></option></select>" + 
			"<div name='"+this.name+"' id='"+this.name+"-slider'/>" + 
			"<input class='servo-value text ui-widget-content ui-corner-all slider-value' type='text' value='0' name='"+this.name+"' id='"+this.name+"-slider-value'/>" +
			"</div>";
}
