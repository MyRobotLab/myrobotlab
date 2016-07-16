#!/usr/bin/env node

//var _ = require('lodash');
var path = require('path');
//var bower = require('bower');
//var async = require('async');
var fs = require('fs');
//var nodefs = require("node-fs");

/*
    @author: LunDev
	Some parts are copied from https://github.com/blittle/bower-installer ,
	which, if worky, would have done a far better job than this script ...
*/

var deleteFolderRecursive = function deleteFolderRecursive(path) {
    var files = [];
    if( fs.existsSync(path) ) {
        files = fs.readdirSync(path);
        files.forEach(function(file,index){
            var curPath = path + "/" + file;
            if(fs.statSync(curPath).isDirectory()) { // recurse
                deleteFolderRecursive(curPath);
            } else { // delete file
                fs.unlinkSync(curPath);
            }
        });
        fs.rmdirSync(path);
    }
};

var copyFile = function copyFile(source, target, cb) {
  //var cbCalled = false;

  var rd = fs.createReadStream(source);
  rd.on("error", function(err) {
    //done(err);
  });
  var wr = fs.createWriteStream(target);
  wr.on("error", function(err) {
    //done(err);
  });
  wr.on("close", function(ex) {
    //done();
  });
  rd.pipe(wr);

  //function done(err) {
    //if (!cbCalled) {
      //cb(err);
      //cbCalled = true;
    //}
  //}
};

var basePath = process.cwd();
var pathSep = '/';

//console.log(path.join(basePath, 'bower.json'));

// Load configuration file
var bowerjson;
try {
	bowerjson = require(path.join(basePath, 'bower.json'));
} catch (e) {
	try {
		bowerjson = require(path.join(basePath, 'component.json'));
	} catch (e) {
		throw new Error('Neither bower.json nor component.json present');
	}
}

var cfg = bowerjson.install;
if (cfg == undefined) {
	throw new Error('Please provide config');
}

//console.log(cfg);

//basePath = ...src/resource/WebGui
//pathSep = /
//cfg.path = app/libgen
//cfg.oldpath = app/bower_components

console.log('Installing into: ' + basePath + pathSep + cfg.path);
console.log('Installing from: ' + basePath + pathSep + cfg.oldpath);

//...src/resource/WebGui   /   app/libgen
deleteFolderRecursive(basePath + pathSep + cfg.path);

try {
	//checks if file exists - throws error if not
    fs.statSync(basePath + pathSep + cfg.path).isDirectory();
} catch(e) {
	fs.mkdirSync(basePath + pathSep + cfg.path);
}

var files = cfg.files;
Object.keys(files).forEach(function(key) {
    var val = files[key];
	//key = angular
	console.log('Now installing: ' + key);
	//...src/resource/WebGui   /   app/libgen   /   angular
	fs.mkdirSync(basePath + pathSep + cfg.path + pathSep + key);
    val.forEach(function(entry) {
        console.log('File: ' + entry);
		//entry = angular.js | dist/js/bootstrap.min.js
		var entry_split = entry.split("/");
		//console.log(entry_split);
		var path_building = '';
		for (var i = 0; i < entry_split.length-1; i++) {
			//console.log(entry_split[i]);
			path_building = path_building + pathSep + entry_split[i];
			//console.log(path_building);
			var path_create = basePath + pathSep + cfg.path + pathSep + key + path_building;
			try {
				//checks if file exists - throws error if not
				fs.statSync(path_create).isDirectory();
			} catch(e) {
				fs.mkdirSync(path_create);
			}
		}
		//...src/resource/WebGui   /   app/bower_components   /   angular   /   angular.js
		var path_orig = basePath + pathSep + cfg.oldpath + pathSep + key + pathSep + entry;
		//...src/resource/WebGui   /   app/libgen   /   angular   /   angular.js
		var path_dest = basePath + pathSep + cfg.path + pathSep + key + pathSep + entry;
		//console.log(path_orig);
		//console.log(path_dest);
		copyFile(path_orig, path_dest);
    });
});

console.log("Done!");


