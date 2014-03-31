// FIXME - must be an easy way to include full gui & debug 

//-------PythonGUI begin---------

function PythonGUI(name) {
	ServiceGUI.call(this, name);
	this.editor = null;
	this.pconsoleData = "";
}

PythonGUI.prototype = Object.create(ServiceGUI.prototype);
PythonGUI.prototype.constructor = PythonGUI;

// --- callbacks begin ---
PythonGUI.prototype.getState = function(data) {
};

PythonGUI.prototype.finishedExecutingScript = function() {
	this.info("finished executing script");
};


PythonGUI.prototype.getScript = function(data) {
	if (data == null){
		this.warn("getScript has null script");
		return;
	} 
	
    var code = data[0].code;
    var filename = data[0].name;
	this.editor.setValue(code);
	$("#python-filename").val(filename);
	this.info("loaded script " + filename);
};

PythonGUI.prototype.getExampleListing = function(data) {
	
	if (data == null){
		this.warn("getExampleListing returned null");
		return;
	}
	
	var files = data[0];
	var filelist = $("#python-example-file-menu");
	for (var i = 0; i < files.length; i++) {
		filelist.append(" <li><a name='"+this.name+"' class='python-example-file' id='" + files[i] + "' href='#'>" + files[i] + "</a></li>");
	}
	
	var menu = $("#python-example-file-menu").menu().hide();
	$(".python-example-file").button().click(function(event) {
		var name = event.currentTarget.name;
		// event.preventDefault();
		var gui = guiMap[name];
		gui.getExampleFile(this.id);
	});

};

PythonGUI.prototype.getFileListing = function(data) {
	
	if (data == null){
		this.warn("getFileListing returned null");
		return;
	}
	
	var files = data[0];
	var filelist = $("#python-file-menu");
	filelist.empty();
	for (var i = 0; i < files.length; i++) {
		filelist.append(" <li><a name='"+this.name+"' class='python-file' id='" + files[i] + "' href='#'>" + files[i] + "</a></li>");
	}
	
	var menu = $("#python-file-menu").menu().hide();
	$(".python-file").button().click(function(event) {
		var name = event.currentTarget.name;
		// event.preventDefault();
		var gui = guiMap[name];
		gui.getFile(this.id);
	});

};

PythonGUI.prototype.getFile = function(data) {
	 //alert(data);
	 $("#python-file-menu").menu().hide();
	 this.send("loadUserScript", [data]);
	 this.send("getScript");
	 // NOTE - there is a broadcast at the end of loadscript from resource
};

PythonGUI.prototype.getExampleFile = function(data) {
	 //alert(data);
	 $("#python-example-file-menu").menu().hide();
	 this.send("loadScriptFromResource", [data]);
	 this.send("getScript");
	 // NOTE - there is a broadcast at the end of loadscript from resource
};


PythonGUI.prototype.publishStdOut = function(data) {
	// fyi scrolltop
	var pconsole = $("#"+this.name+"-console");
	this.pconsoleData = this.pconsoleData + new Date().getTime() + " " + data[0] + "\n";
	pconsole.val(this.pconsoleData)
	// pconsole.prepend(data[0])
	 if (pconsole.val().length > 2048){
		 this.pconsoleData = "";
		 pconsole.val(this.pconsoleData);
	 }
	
	pconsole.scrollTop(pconsole[0].scrollHeight - pconsole.height());
};

//--- callbacks end ---

// --- overrides begin ---
PythonGUI.prototype.attachGUI = function() {
	this.subscribe("publishStatus", "displayStatus"); // TODO DO IN PARENT FRAMEWORK !!!

	//this.subscribe("publishState", "getState"); - trying discreet
	
	this.subscribe("getScript", "getScript");
	this.subscribe("finishedExecutingScript", "finishedExecutingScript");
	this.subscribe("publishStdOut", "publishStdOut");
	this.subscribe("appendScript", "appendScript");
	this.subscribe("startRecording", "startRecording");
	this.subscribe("getExampleListing", "getExampleListing");
	this.subscribe("getFileListing", "getFileListing");
	//this.subscribe("publishState", "getState"); NO WORKY SERIALIZATION ERROR :P
	
	this.send("getExampleListing");
	this.send("getFileListing");
	this.send("attachPythonConsole");
	this.send("getScript");
};

PythonGUI.prototype.detachGUI = function() {
	//this.unsubscribe("publishState", "getState");
};

PythonGUI.prototype.add = function() {
	var oldEl = this.editor.container;
	var pad = document.createElement("div");
	pad.style.padding = "40px";
	oldEl.parentNode.insertBefore(pad, oldEl.nextSibling);

	var el = document.createElement("div")
	oldEl.parentNode.insertBefore(el, pad.nextSibling);

	count++
	this.editor = ace.edit(el)
	this.editor.setTheme(theme)
	this.editor.session.setMode("ace/mode/javascript")

	this.editor.setValue([ "this is editor number: ", count, "\n",
			"using theme \"", theme, "\"\n", ":)" ].join(""), -1)
};


PythonGUI.prototype.init = function() {

	// WTF - this conflicts - but when I remove it ..
	// everything still appears to work !!
	// var $ = document.getElementById.bind(document); - so JQuery is not happy
	// about 'tis
	var dom = require("ace/lib/dom");

	// add command to all new editor instaces
	require("ace/commands/default_commands").commands.push({
		name : "Toggle Fullscreen",
		bindKey : "F11",
		exec : function(editor) {
			dom.toggleCssClass(document.body, "fullScreen")
			dom.toggleCssClass(editor.container, "fullScreen")
			editor.resize()
		}
	}, {
		name : "add",
		bindKey : "Shift-Return",
		exec : add
	})

	// create first editor
	// var editor = ace.edit("editor");
	this.editor = ace.edit("editor");
	var theme = "ace/theme/twilight";
	this.editor.setTheme(theme);
	this.editor.session.setMode("ace/mode/python");

	var count = 1;
	function add() {
		var oldEl = this.editor.container;
		var pad = document.createElement("div");
		pad.style.padding = "40px";
		oldEl.parentNode.insertBefore(pad, oldEl.nextSibling);

		var el = document.createElement("div")
		oldEl.parentNode.insertBefore(el, pad.nextSibling);

		count++
		this.editor = ace.edit(el)
		this.editor.setTheme(theme)
		this.editor.session.setMode("ace/mode/javascript")

		this.editor.setValue([ "this is editor number: ", count, "\n",
				"using theme \"", theme, "\"\n", ":)" ].join(""), -1)

		scroll()
	}

	function scroll(speed) {
		var top = this.editor.container.getBoundingClientRect().top
		speed = speed || 10
		if (top > 60 && speed < 500) {
			if (speed > top - speed - 50)
				speed = top - speed - 50
			else
				setTimeout(scroll, 10, speed + 10)
			window.scrollBy(0, speed)
		}
	}

	setTimeout(function() {
		window.scrollTo(0, 0)
	}, 10);

	// $(function() {
	// $(".python-menu").buttonset().width("300px");
	$(".python-menu").buttonset();
	$("#" + this.name + "-run").button().click(function(event) {
		var name = event.currentTarget.name;
		// event.preventDefault();
		var gui = guiMap[name];
		gui.send("exec", [ gui.editor.getValue() ]);
		// gui.send("exec", [parseInt(this.id), value]);
	});
	$("#" + this.name + "-examples").button().click(function(event) {
		var name = event.currentTarget.name;
		// event.preventDefault();
		var gui = guiMap[name];
		gui.send("getExampleListing");
		// gui.send("exec", [parseInt(this.id), value]);
	});

	$("#" + this.name + "-examples-select").button({
		// text: false,
		icons : {
			primary : "ui-icon-triangle-1-s"
		}
	}).click(function() {
		var menu = $('#python-example-file-menu').toggle();
	});
	

	$("#" + this.name + "-load").button({
		// text: false,
		icons : {
			primary : "ui-icon-triangle-1-s"
		}
	}).click(function() {
		var menu = $('#python-file-menu').toggle();
	});	
	
	
	$("#" + this.name + "-save").button().click(function(event) {
		var name = event.currentTarget.name;
		// event.preventDefault();
		var gui = guiMap[name];
		gui.send("saveAndReplaceCurrentScript",[$("#"+name+"-filename").val(),gui.editor.getValue()]);
		gui.send("getFileListing");
	});
	
	
	/*.click(function() {
		var menu = $('#python-example-file-menu').show().position({
			my : "left top",
			at : "left bottom",
			of : this
		});
		$(document).one("click", function() {
			menu.hide();
		});
		return false;
	}).next().hide().menu();*/
	// });

	// alert(editor.getValue());
};
// --- overrides end ---

// --- gui events begin ---

// --- gui events end ---

PythonGUI.prototype.getPanel = function() {
	return "<div class='python-menu' align='center'>" +
	"<button id='"+this.name+"-run' name='"+this.name+"' >run</button> " +
	"<input type='button' id='"+this.name+"-save' name='"+this.name+"' value='save' /> " +
	//"<input type='button' id='"+this.name+"-examples' name='"+this.name+"' value='examples' /> " +
	"<button id='"+this.name+"-examples-select' name='"+this.name+"'>examples</button>" +
	"  <ul id='python-example-file-menu' >" +
	"  </ul>" +
	"<button  id='"+this.name+"-load' name='"+this.name+"'>load</button>" +
	"  <ul id='python-file-menu' >" +
	"  </ul>" +
	"<input type='text' id='"+this.name+"-filename'  class='text ui-widget-content ui-corner-all' name='"+this.name+"' value='untitled.py' /> " +
	"</div>" +
	"<pre id='editor'>print 'One Software To Rule Them All !!!' \n" +
	"</pre>" +
	"<p align='center' >" +
//	"<div id='"+this.name+"-console' class='ace_content console text ui-widget-content ui-corner-all' style='font-size:x-small;'></div>" +
	"<textarea id='"+this.name+"-console' class='console text ui-widget-content ui-corner-all' style='font-size:x-small;'  rows='10' cols='120'></textarea>" +
	"</p>" + 
	"<div class='scrollmargin'>" +
	"    <div style='padding:20px'>" +
	"        press F11 to switch to fullscreen mode" +
	"    </div>" +
	"    <span onclick='add()' class='large-button' title='Shift+Enter'>+</span>" +
	"</div>" +
	"<script src='/WebGUI/common/ace/src-min/ace.js' type='text/javascript' charset='utf-8'></script>"
	;
}
