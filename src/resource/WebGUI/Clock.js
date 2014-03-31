// FIXME - must be an easy way to include full gui & debug 

//-------ClockGUI begin---------

function ClockGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.
}

ClockGUI.prototype = Object.create(ServiceGUI.prototype);
ClockGUI.prototype.constructor = ClockGUI;

// --- callbacks begin ---
ClockGUI.prototype.pulse = function(data) {
	$("#"+this.name+"-display").html(data);
};

ClockGUI.prototype.getState = function(data) {
	n = this.name;
	$("#"+n+"-display").html(data);
	if (data[0].isClockRunning) {
		$("#"+n+"-startClock").button("option", "label", "stop clock");
	} else {
		$("#"+n+"-startClock").button("option", "label", "start clock");
	}

	$("#"+n+"-interval").val(data[0].interval);
};
//--- callbacks end ---

// --- overrides begin ---
ClockGUI.prototype.attachGUI = function() {
	this.subscribe("pulse", "pulse");
	this.subscribe("publishState", "getState");
	// broadcast the initial state
	this.send("broadcastState");
};

ClockGUI.prototype.detachGUI = function() {
	this.unsubscribe("pulse", "pulse");
	this.unsubscribe("publishState", "getState");
	// broadcast the initial state
};

ClockGUI.prototype.init = function() {
	//alert("#"+this.name+"-startClock");
	$("#"+this.name+"-startClock").button().click(ClockGUI.prototype.startClock);
	$("#"+this.name+"-setInterval").button().click(ClockGUI.prototype.setInterval);

};
// --- overrides end ---

// --- gui events begin ---
ClockGUI.prototype.startClock = function(event) {

	//alert(this.key("startClock"));
	startClock = $("#"+this.name+"-startClock");
	//alert(startClock.attr("name"));
	clockGUI = guiMap[this.name];
	if (startClock.val() == "start clock") {
		startClock.val("stop clock");
		clockGUI.send("startClock", null); // FIXME null shouldn't be required
	} else {
		startClock.val("start clock");
		clockGUI.send("stopClock", null);
	}

	clockGUI.send("broadcastState");
}

ClockGUI.prototype.setInterval = function() {
	clockGUI = guiMap[this.name];
	clockGUI.send("setInterval", [ parseInt($("#"+this.name+"-interval") .val()) ]);
	clockGUI.send("broadcastState");
}
//--- gui events end ---


ClockGUI.prototype.getPanel = function() {
	return "<div>"
			+ "	<div id='"+this.name+"-display'>clock wtf?</div>"
			+ "	<input id='"+this.name+"-startClock' type='button' name='"+this.name+"' value='start clock'/>"
			+ "	<input id='"+this.name+"-setInterval' type='button' name='"+this.name+"' value='set interval'/>"
			+ "	interval <input id='"+this.name+"-interval' type='text' name='"+this.name+"' value='1000'></input>ms"
			+ "</div>";
}
