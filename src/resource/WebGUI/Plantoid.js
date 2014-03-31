// FIXME - must be an easy way to include full gui & debug 

//-------PlantoidGUI begin---------

function PlantoidGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.
}

PlantoidGUI.prototype = Object.create(ServiceGUI.prototype);
PlantoidGUI.prototype.constructor = PlantoidGUI;

// --- callbacks begin ---
PlantoidGUI.prototype.pulse = function(data) {
	$("#"+this.name+"-display").html(data);
};

PlantoidGUI.prototype.getState = function(data) {
	n = this.name;
	$("#"+n+"-display").html(data);
	if (data[0].isPlantoidRunning) {
		$("#"+n+"-startPlantoid").button("option", "label", "stop Plantoid");
	} else {
		$("#"+n+"-startPlantoid").button("option", "label", "start Plantoid");
	}

	$("#"+n+"-interval").val(data[0].interval);
};
//--- callbacks end ---

// --- overrides begin ---
PlantoidGUI.prototype.attachGUI = function() {
	this.subscribe("pulse", "pulse");
	this.subscribe("publishState", "getState");
	// broadcast the initial state
	this.send("broadcastState");
};

PlantoidGUI.prototype.detachGUI = function() {
	this.unsubscribe("pulse", "pulse");
	this.unsubscribe("publishState", "getState");
	// broadcast the initial state
};

PlantoidGUI.prototype.init = function() {
	//alert("#"+this.name+"-startPlantoid");
	$("#"+this.name+"-startPlantoid").button().click(PlantoidGUI.prototype.startPlantoid);
	$("#"+this.name+"-setInterval").button().click(PlantoidGUI.prototype.setInterval);

};
// --- overrides end ---

// --- gui events begin ---
PlantoidGUI.prototype.startPlantoid = function(event) {

	//alert(this.key("startPlantoid"));
	startPlantoid = $("#"+this.name+"-startPlantoid");
	//alert(startPlantoid.attr("name"));
	PlantoidGUI = guiMap[this.name];
	if (startPlantoid.val() == "start Plantoid") {
		startPlantoid.val("stop Plantoid");
		PlantoidGUI.send("startPlantoid", null); // FIXME null shouldn't be required
	} else {
		startPlantoid.val("start Plantoid");
		PlantoidGUI.send("stopPlantoid", null);
	}

	PlantoidGUI.send("broadcastState");
}

PlantoidGUI.prototype.setInterval = function() {
	PlantoidGUI = guiMap[this.name];
	PlantoidGUI.send("setInterval", [ parseInt($("#"+this.name+"-interval") .val()) ]);
	PlantoidGUI.send("broadcastState");
}
//--- gui events end ---


PlantoidGUI.prototype.getPanel = function() {
	return "<div>"
			+ "	<div id='"+this.name+"-display'>Plantoid wtf?</div>"
			+ "	<input id='"+this.name+"-startPlantoid' type='button' name='"+this.name+"' value='start Plantoid'/>"
			+ "	<input id='"+this.name+"-setInterval' type='button' name='"+this.name+"' value='set interval'/>"
			+ "	interval <input id='"+this.name+"-interval' type='text' name='"+this.name+"' value='1000'></input>ms"
			+ "</div>";
}
