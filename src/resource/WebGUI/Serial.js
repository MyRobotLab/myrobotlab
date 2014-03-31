// FIXME - must be an easy way to include full gui & debug 

//-------SerialGUI begin---------

function SerialGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.	
	this.portNames = [];
	this.consoleData = "";
	this.serialConsole = null;
}

SerialGUI.prototype = Object.create(ServiceGUI.prototype);
SerialGUI.prototype.constructor = SerialGUI;

// --- callbacks begin ---
SerialGUI.prototype.getState = function(data) {
	n = this.name;
	
	var serial = data[0]; // FIXME - this should be in registry ???	
	var ports = this.portNames; // FIXME - interface ??? how ??? Arduino has serialDeviceNames 
	var connected = serial.connected;
	
	// ports begin ---
	$("#"+this.name+"-ports")
    .find('option')
    .remove()
    .end();
	
	$("#"+this.name+"-ports").append("<option value=''></option>");
	for (var i = 0; i < ports.length; i++) {
		$("#"+this.name+"-ports").append("<option value='"+ports[i]+"' "+((serial.portName == ports[i])?"selected":"") +">"+ports[i]+"</option>");
	}
	
	if (connected) {
		$("#"+this.name+"-connected").attr("src","/WebGUI/common/button-green.png");
	} else {
		$("#"+this.name+"-connected").attr("src","/WebGUI/common/button-red.png");
	}
	// ports end ---
        
};


SerialGUI.prototype.publishByte = function(data) {

	this.consoleData = this.consoleData + data[0];
	this.serialConsole.val(this.consoleData)
	// pconsole.prepend(data[0])
	
	 if (this.consoleData.length > 4096){
		 this.consoleData = "";
		 this.serialConsole.val(this.consoleData);
	 }
	 
	
	this.serialConsole.scrollTop(this.serialConsole[0].scrollHeight - this.serialConsole.height());
}

SerialGUI.prototype.getPortNames = function(data) {
	this.portNames = data[0];
}


//--- callbacks end ---

// --- overrides begin ---
SerialGUI.prototype.attachGUI = function() {
	this.subscribe("publishStatus", "displayStatus"); // TODO DO IN PARENT FRAMEWORK !!!
	
	this.subscribe("publishState", "getState");
	this.subscribe("publishByte", "publishByte");
	this.subscribe("getPortNames", "getPortNames");
	// broadcast the initial state
	
	//this.send("getTargetsTable");
	this.send("getPortNames");
	this.send("broadcastState");
};

SerialGUI.prototype.detachGUI = function() {
	// broadcast the initial state
};

SerialGUI.prototype.sendData = function() {
	var data = $("#"+this.name+"-input").val();
	this.send("write", [data]);
}


SerialGUI.prototype.init = function() {
	
	var gui = guiMap[this.name]; // WTF reference back to self? - heh Oh yeah..
	$("#"+this.name+"-ports").change(function() {
		  gui.connect();
	});
	
	$("#"+this.name+"-send").button().click(function() {
		  gui.sendData();
	});
	
	this.serialConsole = $("#"+this.name+"-console");
	
	
};
// --- overrides end ---

// --- gui events begin ---
SerialGUI.prototype.connect = function() {
	var port = $("#"+this.name+"-ports").find(":selected").text();
	this.send("connect", new Array(port, 57600, 8, 1, 0));
	this.send("broadcastState");
}
//--- gui events end ---


SerialGUI.prototype.getPanel = function() {
	 
	
	var ret = "  <label>Port: </label>" +
	"  <select class='text ui-widget-content ui-corner-all' id='"+this.name+"-ports' name='"+this.name+"'>" +
	"    <option value=''>Select one...</option>" +
	"  </select>" + 
	"    <img id='"+this.name+"-connected' name='"+this.name+"' src='/WebGUI/common/button-red.png' /><br/>" +
	"<textarea id='"+this.name+"-console' class='console text ui-widget-content ui-corner-all' style='font-size:x-small;'  rows='30' cols='160'></textarea><br/>" +
	"<textarea id='"+this.name+"-input' class='console text ui-widget-content ui-corner-all' style='font-size:x-small;'  rows='2' cols='160'></textarea><br/>"  +
	"<button id='"+this.name+"-send'>send</button>"
	;
	
	return ret;
}
