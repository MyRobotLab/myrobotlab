// FIXME - must be an easy way to include full gui & debug 

//-------PickToLightGUI begin---------

function PickToLightGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.
}

PickToLightGUI.prototype = Object.create(ServiceGUI.prototype);
PickToLightGUI.prototype.constructor = PickToLightGUI;

// --- callbacks begin ---
PickToLightGUI.prototype.pulse = function(data) {
	$("#"+this.name+"-display").html(data);
};

PickToLightGUI.prototype.getState = function(data) {
	n = this.name;
	$("#"+n+"-display").html(data);
	if (data[0].isPickToLightRunning) {
		$("#"+n+"-startPickToLight").button("option", "label", "stop PickToLight");
	} else {
		$("#"+n+"-startPickToLight").button("option", "label", "start PickToLight");
	}

	$("#"+n+"-interval").val(data[0].interval);
};
//--- callbacks end ---

// --- overrides begin ---
PickToLightGUI.prototype.attachGUI = function() {
	this.subscribe("pulse", "pulse");
	this.subscribe("publishState", "getState");
	// broadcast the initial state
	this.send("broadcastState");
};

PickToLightGUI.prototype.detachGUI = function() {
	this.unsubscribe("pulse", "pulse");
	this.unsubscribe("publishState", "getState");
	// broadcast the initial state
};

PickToLightGUI.prototype.init = function() {
	//alert("#"+this.name+"-startPickToLight");
	$("#"+this.name+"-startPickToLight").button().click(PickToLightGUI.prototype.startPickToLight);
	$("#"+this.name+"-setInterval").button().click(PickToLightGUI.prototype.setInterval);

};
// --- overrides end ---

// --- gui events begin ---
PickToLightGUI.prototype.startPickToLight = function(event) {

	//alert(this.key("startPickToLight"));
	startPickToLight = $("#"+this.name+"-startPickToLight");
	//alert(startPickToLight.attr("name"));
	PickToLightGUI = guiMap[this.name];
	if (startPickToLight.val() == "start PickToLight") {
		startPickToLight.val("stop PickToLight");
		PickToLightGUI.send("startPickToLight", null); // FIXME null shouldn't be required
	} else {
		startPickToLight.val("start PickToLight");
		PickToLightGUI.send("stopPickToLight", null);
	}

	PickToLightGUI.send("broadcastState");
}

PickToLightGUI.prototype.setInterval = function() {
	PickToLightGUI = guiMap[this.name];
	PickToLightGUI.send("setInterval", [ parseInt($("#"+this.name+"-interval") .val()) ]);
	PickToLightGUI.send("broadcastState");
}
//--- gui events end ---


PickToLightGUI.prototype.getPanel = function() {
	return "<div>"
			+ "	<div id='"+this.name+"-display'>PickToLight wtf?</div>"
			+ "	<input id='"+this.name+"-startPickToLight' type='button' name='"+this.name+"' value='start PickToLight'/>"
			+ "	<input id='"+this.name+"-setInterval' type='button' name='"+this.name+"' value='set interval'/>"
			+ "	interval <input id='"+this.name+"-interval' type='text' name='"+this.name+"' value='1000'></input>ms"
			+ "</div>";
}
