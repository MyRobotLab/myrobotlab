// FIXME - must be an easy way to include full gui & debug 

//-------ArduinoGUI begin---------

function ArduinoGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.
	this.sweep = 1;
	this.canvas = null;
    this.context = null;
    this.oscopeWidth = 800;
    this.oscopeHeight = 600;
    this.traceData = [];
    this.pinList = null;
    this.portNames = [];
    this.portNames = null;
}

ArduinoGUI.prototype = Object.create(ServiceGUI.prototype);
ArduinoGUI.prototype.constructor = ArduinoGUI;

// --- callbacks begin ---
ArduinoGUI.prototype.getState = function(data) {
	n = this.name;
	
	var arduino = data[0];	
	var boards = data[0].targetsTable.arduino.boards;
	var boardType = arduino.boardType;
	//var ports = this.portNames;
	this.portName = arduino.portName;
	var connected = arduino.connected;
	
	if (connected) {
		$("#"+this.name+"-connected").attr("src","/WebGUI/common/button-green.png");
		this.send("getVersion");
	} else {
		$("#"+this.name+"-connected").attr("src","/WebGUI/common/button-red.png");
	}
	
	// boards begin ---
	$("#"+this.name+"-boards")
    .find('option')
    .remove()
    .end();
	
	$("#"+this.name+"-boards").append("<option value=''></option>");	
	for (var board in boards) {
		$("#"+this.name+"-boards").append("<option value='"+board+"' "+((boardType == board)?"selected":"") +">"+boards[board].name+"</option>");
	}
	// boards end ---
	// pin list begin ---	
	this.pinList = arduino.pinList;
	$("#"+this.name+"-pinList").empty();
	var analogPinCount = 0;
	var pinLabel = "";
	for (var i = 2; i < this.pinList.length; i++) {
		console.log(this.pinList[i]);
		var pin = this.pinList[i];
		
		if (pin.type == 1){
			pinLabel = "D" + i;
		} else if (pin.type == 2) {
			pinLabel = "PWM" + i;
		} else if (pin.type == 3) {
			pinLabel = "A" + analogPinCount;
			++analogPinCount;
		} else {
			pinLabel = "?" + i;
		}		
		
		// FIXME - will need this.name in all identifier fields to be unique across 
		// multiple arduinos...
		$("#"+this.name+"-pinList").append(
				"<div class='pin-set'><img id='"+this.name+"-pin-"+i+"-led' src='/WebGUI/common/button-small-"+ ((pin.value == 1)?"green":"grey") +".png' />" +
				"<input type='checkbox' class='pin' name='"+this.name+"' id='"+i+"' "+ ((pin.value == 1)?"checked":"") +"/><label for='"+i+"'>" +pinLabel+ "</label>" +
				"<input type='button' class='pinmode' value='out' pidId='"+i+"' name='"+this.name+"' id='"+i+"-test' />" +
				((pin.type == 2)?"<div class='pwm' name='"+this.name+"' pwmId='"+i+"' id='"+i+"-slider'/><input class='pwm-value text ui-widget-content ui-corner-all slider-value' type='text' value='0' name='"+this.name+"' id='"+i+"-slider-value'/>":"") +
						"</div>" +
						"</div>");
	}
	
	$(function() {
	    $(".pin-set").buttonset().width("300px");
	    $(".pin").click(function( event ) {
	       // event.preventDefault();
	        var value = (this.checked)?1:0;
		    var gui = guiMap[this.name];
		    gui.send("digitalWrite", [parseInt(this.id), value]);
		    $("#" + this.name+"-pin-"+this.id+"-led").attr("src",((this.checked)?"/WebGUI/common/button-small-green.png":"/WebGUI/common/button-small-grey.png"));
	    	
	      });
	  });
	
	$(function() {
	    $(".pwm").slider({ 
	    	max: 255,
	    	slide: function( event, ui ) {

	    		var pwmID = $(this).attr("pwmId");
	    		var gui = guiMap[$(this).attr("name")];
	    		gui.send("analogWrite",[parseInt(pwmID), ui.value])
	    		$("#"+pwmID+"-slider-value").val(ui.value);
	          }
	    });
	  });
	
    // FIXME - get data from registry - 
	// parent class "getState" should update registry !!!
	// need to get pinList to determine pin type
	// analog vs digital polling
    $(".pinmode").click(function(){
    	var gui = guiMap[$(this).attr("name")];
    	//var pin = pinList[]
    	if ($(this).val() == "in"){
    		$(this).val("out");
    		gui.send("pinMode",[1]);
    	} else {
    		$(this).val("in");
    		gui.send("pinMode",[0]);
    		//gui.send((pin.type)?"d",[0]);
    	}
   });
	// pin list end ---	
        
};


function TraceData(pin, color)
{
	this.pin=pin;
	this.color=color;
	this.data=[];
	this.index = 0;
}

// gratefully lifted from - http://www.html5rocks.com/en/tutorials/canvas/texteffects/
//HSL (1978) = H: Hue / S: Saturation / L: Lightness
HSL_RGB = function (o) { // { H: 0-360, S: 0-100, L: 0-100 }
  var H = o.H / 360,
      S = o.S / 100,
      L = o.L / 100,
      R, G, B, _1, _2;

  function Hue_2_RGB(v1, v2, vH) {
    if (vH < 0) vH += 1;
    if (vH > 1) vH -= 1;
    if ((6 * vH) < 1) return v1 + (v2 - v1) * 6 * vH;
    if ((2 * vH) < 1) return v2;
    if ((3 * vH) < 2) return v1 + (v2 - v1) * ((2 / 3) - vH) * 6;
    return v1;
  }

  if (S == 0) { // HSL from 0 to 1
    R = L * 255;
    G = L * 255;
    B = L * 255;
  } else {
    if (L < 0.5) {
      _2 = L * (1 + S);
    } else {
      _2 = (L + S) - (S * L);
    }
    _1 = 2 * L - _2;

    R = 255 * Hue_2_RGB(_1, _2, H + (1 / 3));
    G = 255 * Hue_2_RGB(_1, _2, H);
    B = 255 * Hue_2_RGB(_1, _2, H - (1 / 3));
  }

  return {
    R: R,
    G: G,
    B: B
  };
};

ArduinoGUI.prototype.publishPin = function(data) {
	var pin = data[0];
	if (this.traceData[pin.pin] == null)
	{   // get pin count and amoratize over 360
		var hue = Math.floor(360 * (pin.pin / this.pinList.length));
		this.traceData[pin.pin] = new TraceData(hue, pin.pin);
	}
	
	var tdata = this.traceData[pin.pin];
	tdata.data[tdata.index] = pin.value;
	
    this.context.beginPath();
    if (tdata.data[tdata.index-1]!=null){
    	this.context.moveTo(tdata.index-1, tdata.data[tdata.index-1]);
    } else {
    	this.context.moveTo(0, tdata.data[0]);
    }
    
    this.context.lineTo(tdata.index, tdata.data[tdata.index]);
    //context.lineWidth = 10;
    
    this.context.strokeStyle = "hsl(" + (tdata.color) + ",99%,50%)";
    this.context.stroke();
    ++tdata.index;
    if (tdata.index%this.oscopeWidth == 0){
    	tdata.index = 0;
    	this.context.fillStyle="#999999";
    	this.context.fillRect(0,0,this.oscopeWidth,this.oscopeHeight);
    }
}

ArduinoGUI.prototype.getVersion = function(data) {
	if (data == null){
		this.warn("getVersion returned null");
		return;
	} else {
		$("#"+this.name+"-firmware-version").text(data[0]);
	}
}

ArduinoGUI.prototype.getPortNames = function(data) {
	var ports = data[0];
	
	// ports begin ---
	$("#"+this.name+"-ports")
    .find('option')
    .remove()
    .end();
	
	$("#"+this.name+"-ports").append("<option value=''></option>");
	for (var i = 0; i < ports.length; i++) {
		$("#"+this.name+"-ports").append("<option value='"+ports[i]+"' "+((this.portName == ports[i])?"selected":"") +">"+ports[i]+"</option>");
	}
	
}

//--- callbacks end ---

// --- overrides begin ---
ArduinoGUI.prototype.attachGUI = function() {
	this.subscribe("publishStatus", "displayStatus"); // TODO DO IN PARENT FRAMEWORK !!!
	//	this.subscribe("publishStatus", "publishStatus"); // YOU CAN'T DO THIS - because its a parent defined method
	//this.subscribe("publishStatus", "error"); // YOU CAN'T DO THIS - because its a parent defined method!!! (AGAIN!!!)
	
	this.subscribe("getTargetsTable", "getTargetsTable");
	this.subscribe("publishState", "getState");
	this.subscribe("publishPin", "publishPin");
	this.subscribe("getVersion", "getVersion");
	this.subscribe("getPortNames", "getPortNames");
	// broadcast the initial state
	
	//this.send("getTargetsTable");
	this.send("broadcastState"); // FIXME - lame big hammer
	this.send("getPortNames");
};

ArduinoGUI.prototype.detachGUI = function() {
	// broadcast the initial state
};

ArduinoGUI.prototype.init = function() {
	
	//$(document).ready(function(){
	
	var gui = guiMap[this.name];
	$("#"+this.name+"-ports").change(function() {
		//var port = $("#"+this.name+"-ports").find(":selected").text();
		  gui.connect();
	});
	
	$("#"+this.name+"-menu").buttonset();
	
	// finally - http://jsfiddle.net/loktar/q7Z9k/ - someone who knows how to load an image
	this.canvas = document.getElementById(this.name + "-oscope");
	this.context = this.canvas.getContext('2d');
	
	//$("#"+this.name+"-oscope-container").hide();
	
	jqcanvas = $("#"+this.name+"-oscope");
	jqcanvas.attr("width", this.oscopeWidth);
	jqcanvas.attr("height", this.oscopeHeight);
	//this.context
	
	// dumb - yet extremely necessary as background.onload can not refer to "this.context" - but can 
	// refer to current scope :P
	var context = this.context;
	// background - begin ---
	var background = new Image();
	// 750 x 582
	background.src = "http://softsolder.files.wordpress.com/2008/12/dsc00809-hp54602b-serial-setup-screenshot.jpg";
	background.onload = function(){
			// context.drawImage(background,0,0);   // DRAW AFTER LOAD ! - TODO draw after load
	}
	
	this.context.fillStyle="#999999";
	this.context.fillRect(0,0,this.oscopeWidth,this.oscopeHeight);
	
	
	// background - begin ---
};
// --- overrides end ---

// --- gui events begin ---
ArduinoGUI.prototype.connect = function() {
	var port = $("#"+this.name+"-ports").find(":selected").text();
	this.send("connect", new Array(port, 57600, 8, 1, 0));
}
//--- gui events end ---


ArduinoGUI.prototype.getPanel = function() {
	var ret =  "<div class='ui-widget'>" +
	// oscope
	"<div id='"+this.name+"-oscope-container'>" +
	"<canvas class='oscope' id='"+this.name+"-oscope' width='750' height='582'></canvas>" +
	"</div>" + 
	//" <img src='/WebGUI/Arduino/arduino.duemilanove.200.pins.png' />" +
	"</div>"  +
	
	"<div name='"+this.name+"' id='"+this.name+"-menu'>" +
	"  <input type='checkbox' name='"+this.name+"' id='"+this.name+"-oscope-toggle' /><label for='"+this.name+"-oscope-toggle'>oscope</label>" +
	"  <input type='checkbox' name='"+this.name+"' id='"+this.name+"-pinList-toggle' /><label for='"+this.name+"-pinList-toggle'>pinlist</label>" +
	"  <input type='checkbox' name='"+this.name+"' id='"+this.name+"-refresh-toggle' /><label for='"+this.name+"-refresh-toggle'>refresh</label>" +
	"</div>" +
	
	//" Status: <label id='"+this.name+"-status'></label><br/>" +
	"  <label>Port: </label>" +
	"  <select class='text ui-widget-content ui-corner-all' id='"+this.name+"-ports' name='"+this.name+"'>" +
	"    <option value=''>Select one...</option>" +
	"  </select>" +
	"    <img id='"+this.name+"-connected' name='"+this.name+"' src='/WebGUI/common/button-red.png' />"+

	"  <label>Board: </label>" +
	"  <select class='text ui-widget-content ui-corner-all' id='"+this.name+"-boards' name='"+this.name+"'>" +
	"    <option value=''>Select one...</option>" +
	"  </select><br/>" +
	"<label>firmware version </label> <label class='text ui-widget-content ui-corner-all' value='' id='"+this.name+"-firmware-version'></label>" +

	// pin list
	"<div id='"+this.name+"-pinList'></div>" 
	;
	
	return ret;
}
