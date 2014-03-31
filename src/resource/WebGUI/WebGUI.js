// FIXME - must be an easy way to include full gui & debug 

//-------WebGUIGUI begin---------

function WebGUIGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.
}

WebGUIGUI.prototype = Object.create(ServiceGUI.prototype);
WebGUIGUI.prototype.constructor = WebGUIGUI;

// --- callbacks begin ---
// FIXME - a getState is going to be more recent from a getRegistry -
// put in registry (super ?)
WebGUIGUI.prototype.getState = function(data) {
	n = this.name;
	var webgui = data[0];
	$("#"+n+"-httpPort").val(webgui.httpPort);
	$("#"+n+"-wsPort").val(webgui.wsPort);
	
	$("#clients").empty();
	// clients modify myrobotlab html
	for (var key in webgui.clients) { 
		$("#clients").append(key + "<br/>");
	}
};
//--- callbacks end ---

// --- overrides begin ---
WebGUIGUI.prototype.attachGUI = function() {
	this.subscribe("publishStatus", "displayStatus");
	this.subscribe("publishState", "getState");
	// broadcast the initial state
	this.send("broadcastState");
};

WebGUIGUI.prototype.detachGUI = function() {
	this.unsubscribe("publishState", "getState");
};

WebGUIGUI.prototype.init = function() {	
	$("#"+this.name+"-setPorts").button().click(WebGUIGUI.prototype.setPorts);
	$("#"+this.name+"-customize").button().click(WebGUIGUI.prototype.customize);
};
// --- overrides end ---

// --- gui events begin ---
WebGUIGUI.prototype.setPorts = function(event) {

	var gui = guiMap[this.name];
	var httpPort = $("#"+this.name+"-httpPort").val();
	var wsPort	 = $("#"+this.name+"-wsPort").val();
	gui.send("startWebServer", [parseInt(httpPort)]);
	gui.send("startWebSocketServer", [parseInt(wsPort)]);
	//gui.send("broadcastState");
}

WebGUIGUI.prototype.customize = function(event) {

	var gui = guiMap[this.name];
	gui.send("customize");
}

//--- gui events end ---


WebGUIGUI.prototype.getPanel = function() {
	return "<div>"
			+ "	http port       <input class='text ui-widget-content ui-corner-all' id='"+this.name+"-httpPort' type='text' value=''/>"
			+ "	web socket port <input class='text ui-widget-content ui-corner-all' id='"+this.name+"-wsPort' type='text' value=''/>"
			+ "	<input id='"+this.name+"-setPorts' name='"+this.name+"' type='button' value='set'/>"
			+ "	<input id='"+this.name+"-customize' name='"+this.name+"' type='button' value='customize'/>"
			+ "</div>";
}
