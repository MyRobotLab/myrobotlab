// FIXME - must be an easy way to include full gui & debug 

//-------SerialGUI begin---------

function SerialGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.	
	this.portNames = [];
	this.consoleData = "";
	this.serialConsole = null;
	this.rxFormat = "decimal";
	this.txFormat = "decimal";
	this.rxCount = 0;
	this.txCount = 0;
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
	
	$("#"+this.name+"-rate").val(serial.rate);
	
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

	var num = data[0];
	++this.rxCount;
	
	$("#"+this.name+"-rx-count").text(this.rxCount);
	
	if (this.rxFormat == "decimal") {
		var s = "000" + data[0];
		num = " " + s.substr(s.length-3);
	} else if (this.rxFormat == "ascii")  {
		num = String.fromCharCode(num);
	} else {
		// hex
		num = " " + parseInt(num).toString(16);
	}
	    
	this.consoleData = this.consoleData + num;
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
	
	this.send("getPortNames");
	this.send("broadcastState");
};

SerialGUI.prototype.detachGUI = function() {
	// broadcast the initial state
};

// send button - data
SerialGUI.prototype.sendData = function() {
	var gui = guiMap[this.name];	
	var data = $("#"+this.name+"-input").val();
	gui.txCount += data.length;
	$("#"+this.name+"-tx-count").text(gui.txCount);
	this.send("write", [data]);
}


SerialGUI.prototype.init = function() {
	
	var gui = guiMap[this.name]; // WTF reference back to self? - heh Oh yeah..
	
	// ---- events begin ----
	// port connect
	$("#"+this.name+"-ports").change(function() {
		  gui.connect();
	});
	
	// rate connect
	$("#"+this.name+"-rate").change(function() {
		  gui.connect();
	});
	
	// button send
	$("#"+this.name+"-send").button().click(function() {
		  gui.sendData();
	});
	
	// rx format
	$("#"+this.name+"-rx-display").change(function() {
		var gui = guiMap[this.name]; 
		gui.rxFormat = this.value;
	});
	
	// tx format
	$( "#"+this.name+"-tx-display").change(function() {
		var gui = guiMap[this.name]; 
		gui.txFormat = this.value;
	});
	// ---- events ends ----

	// attach console
	this.serialConsole = $("#"+this.name+"-console");	
};
// --- overrides end ---

// --- gui events begin ---
SerialGUI.prototype.connect = function() {
	var gui = guiMap[this.name]; // WTF reference back to self? - heh Oh yeah..
	var port = $("#"+this.name+"-ports").find(":selected").text();
	var rate = $("#"+this.name+"-rate").find(":selected").text();
	this.send("connect", new Array(port, parseInt(rate), 8, 1, 0));
	this.send("broadcastState");
}
//--- gui events end ---


SerialGUI.prototype.getPanel = function() {
	 
	var ret = "    <img id='"+this.name+"-connected' name='"+this.name+"' src='/WebGUI/common/button-red.png' /><br/>" +
		"  <label>port </label>" +
	"  <select class='text ui-widget-content ui-corner-all' id='"+this.name+"-ports' name='"+this.name+"'>" +
	"    <option value=''>Select one...</option>" +
	"  </select>" + 
	"  <select class='text ui-widget-content ui-corner-all' id='"+this.name+"-rate' name='"+this.name+"' value='57600'>" +
	"    <option value='115200'>115200</option>" +
	"    <option value='57600'>57600</option>" +
	"    <option value='38400'>38400</option>" +
	"    <option value='19200'>19200</option>" +
	"    <option value='9600'>9600</option>" +
	"    <option value='4800'>4800</option>" +
	"    <option value='2400'>2400</option>" +
	"    <option value='1200'>1200</option>" +
	"  </select>" + 
	"<label>rx </label><label id='"+this.name+"-rx-count' name='"+this.name+"-rx-count'>0</label>" +
	"  <select class='text ui-widget-content ui-corner-all' id='"+this.name+"-rx-display' name='"+this.name+"' value='decimal'>" +
	"    <option value='decimal'>decimal</option>" +
	"    <option value='ascii'>ascii</option>" +
	"    <option value='hex'>hex</option>" +
	"  </select>" + 
	"<label>tx </label><label id='"+this.name+"-tx-count' name='"+this.name+"-tx-count'>0</label>" +
	"  <select class='text ui-widget-content ui-corner-all' id='"+this.name+"-tx-display' name='"+this.name+"' value='decimal'>" +
	"    <option value='decimal'>decimal</option>" +
	"    <option value='ascii'>ascii</option>" +
	"    <option value='hex'>hex</option>" +
	"  </select>" + 	
	"<br/><textarea id='"+this.name+"-console' class='console text ui-widget-content ui-corner-all' style='font-size:x-small;'  rows='30' cols='160' readonly></textarea><br/>" +
	"<textarea id='"+this.name+"-input' class='console text ui-widget-content ui-corner-all' style='font-size:x-small;'  rows='2' cols='160'></textarea><br/>"  +
	"<button id='"+this.name+"-send'>send</button>"
	;
	
	return ret;
}
