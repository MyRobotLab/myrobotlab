angular.module('mrlapp.service.ServoGui', []).controller('ServoGuiCtrl', ['$log', '$timeout', '$scope', 'mrl', function($log, $timeout, $scope, mrl) {
    $log.info('ServoGuiCtrl')
    var _self = this
    var msg = this.msg

    var firstTime = true

    // init
    $scope.controller = null
    $scope.pinsList = []
    $scope.pin = null
    $scope.min = 0
    $scope.max = 180

    $scope.possibleController = null
    $scope.testTime = 300
    $scope.sliderEnabled = false
    $scope.properties = []
    $scope.speed = null

    $scope.showProperties = false

    // TODO - should be able to build this based on
    // current selection of controller
    $scope.pinList = []
    //slider config with callbacks
    $scope.pos = {
        value: 90,
        options: {
            floor: 0,
            ceil: 180,
            minLimit: 0,
            maxLimit: 180,
            onStart: function() {},
            onChange: function() {
                if ($scope.sliderEnabled) {
                    msg.send('moveTo', $scope.pos.value)
                }
            },
            onEnd: function() {}
        }
    }

    $scope.limits = {
        minValue: 0,
        maxValue: 180,
        options: {
            floor: 0,
            ceil: 180,
            step: 1,
            showTicks: false,
            onStart: function() {},
            /* - changing only on mouse up event - look in ServoGui.html - cannot do this !!! - sliding to the end an letting go doesnt do what you expect */
            onChange: function() {
                msg.send('setMinMax', $scope.limits.minValue, $scope.limits.maxValue)
            },
            onEnd: function() {}
        }
    }

    $scope.setSpeed = function(speed) {
        if (speed == null || speed.trim().length == 0) {
            msg.send("unsetSpeed")
        } else {
            msg.send("setSpeed", speed)
        }
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        if (service.targetPos == null) {// $scope.pos.value = service.rest
        } else {// $scope.pos.value = service.targetPos            
        }

        if (service.controller != null) {
            $scope.possibleController = service.controller
        }
        $scope.controller = service.controller
        $scope.speed = service.speed
        $scope.pin = service.pin
        $scope.rest = service.rest

        if (firstTime) {
            $scope.pos.value = service.currentPos
            $scope.sliderEnabled = true
            firstTime = false
        }

        // set min/max mapper slider BAD IDEA !!!! control "OR" status NEVER BOTH !!!!
        $scope.limits.minValue = service.mapper.minX
        $scope.limits.maxValue = service.mapper.maxX
        $scope.pinList = service.pinList
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            _self.setProperties(data)
            $scope.$apply()
            break
            // servo event in the past 
            // meant feedback from MRLComm.c
            // but perhaps its come to mean
            // feedback from the service.moveTo
        case 'onServoData':
            $scope.service.currentPos = data.pos
            $scope.$apply()
            break
        case 'onStatus':
            $scope.status = data
            $scope.$apply()
            break
        case 'addListener':
            // wtf?
            $log.info("Add listener called")
            $scope.status = data
            $scope.$apply()
            break
        default:
            $log.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    }

    $scope.getSelectionBarColor = function() {
        return "black"
    }

    $scope.isAttached = function() {
        return $scope.service.controller != null
    }

    $scope.update = function(speed, rest, min, max) {
        msg.send("setSpeed", speed)
        msg.send("setRest", rest)
        msg.send("setMinMax", min, max)
    }

    $scope.setPin = function(inPin) {
        $scope.pin = inPin
    }

    // regrettably the onMethodMap dynamic
    // generation of methods failed on this overloaded
    // sweep method - there are several overloads in the
    // Java service - although msg.sweep() was tried for ng-click
    // for some reason Js resolved msg.sweep(null, null, null, null) :P
    $scope.sweep = function() {
        msg.send('sweep')
    }
    $scope.setSelectedController = function(name) {
        $log.info('setSelectedController - ' + name)
        $scope.selectedController = name
        $scope.controller = name
    }
    $scope.attachController = function() {
        $log.info("attachController")
        msg.send('attach', $scope.possibleController, $scope.pin, $scope.rest)
        // msg.attach($scope.controller, $scope.pin, 90)
    }

    // FIXME - put this in mrl service
    // lovely function - https://stackoverflow.com/questions/19098797/fastest-way-to-flatten-un-flatten-nested-json-objects
    this.flatten = function(data) {
        var result = {};
        function recurse(cur, prop) {
            if (Object(cur) !== cur) {
                result[prop] = cur;
            } else if (Array.isArray(cur)) {
                for (var i = 0, l = cur.length; i < l; i++)
                    recurse(cur[i], prop + "[" + i + "]");
                if (l == 0)
                    result[prop] = [];
            } else {
                var isEmpty = true;
                for (var p in cur) {
                    isEmpty = false;
                    recurse(cur[p], prop ? prop + "." + p : p);
                }
                if (isEmpty && prop)
                    result[prop] = {};
            }
        }
        recurse(data, "");
        return result;
    }

    this.unflatten = function(data) {
        "use strict";
        if (Object(data) !== data || Array.isArray(data))
            return data;
        var regex = /\.?([^.\[\]]+)|\[(\d+)\]/g
          , resultholder = {};
        for (var p in data) {
            var cur = resultholder, prop = "", m;
            while (m = regex.exec(p)) {
                cur = cur[prop] || (cur[prop] = (m[2] ? [] : {}));
                prop = m[2] || m[1];
            }
            cur[prop] = data[p];
        }
        return resultholder[""] || resultholder;
    }

    msg.subscribe("publishMoveTo")
    msg.subscribe("publishServoData")
    msg.subscribe(this)

    this.setProperties = function(service) {

        let flat = this.flatten(service)
        console.table(flat)

        $scope.properties = [];

        let exclude = {
            id: "id",
            simpleName: "simpleName",
            creationOrder: "creationOrder",
            "serviceType.name": "serviceType.name",
            "serviceType.simpleName": "serviceType.simpleName",
            "serviceType.isCloudService": "serviceType.isCloudService",
            "serviceType.includeServiceInOneJar": "serviceType.includeServiceInOneJar",
            "serviceType.description": "serviceType.description",
            "serviceType.available": "serviceType.available",
            "interfaceSet.org.myrobotlab.service.interfaces.ServoControl": "interfaceSet.org.myrobotlab.service.interfaces.ServoControl",
            // this will need work :P
            serviceClass: "serviceClass",
            name: "name",
            statusBroadcastLimitMs: "statusBroadcastLimitMs",
            interfaceSet: "interfaceSet",
            isRunning: "isRunning"
        }

        let info = {
            autoDisable: "servo will de-energize if no activity occurs in {idleTimeout} ms - saving the servo from unnecessary wear or damage",
            idleTimeout: "number of milliseconds the servo will de-energize if no activity has occurred",
            isSweeping: "servo is in sweep mode - which will make the servo swing back and forth at current speed between min and max values",
            lastActivityTimeTs: "timestamp of last move servo did"
        }

        // Push each JSON Object entry in array by [key, value]
        for (var i in flat) {

            let o = flat[i]
            if (typeof o == "object") {
                console.log('ere')
            }

            if (i in exclude) {
                continue
            }

            let inf = (info[i] == null) ? '' : info[i]

            $scope.properties.push([i, flat[i], inf]);
        }

        // Run native sort function and returns sorted array.
        return $scope.properties.sort();
    }

    // msg.send('broadcastState')

    // no longer needed - interfaces now travel with a service
    // var runtimeName = mrl.getRuntime().name
    // mrl.subscribe(runtimeName, 'getServiceNamesFromInterface')
    // mrl.subscribeToServiceMethod(this.onMsg, runtimeName, 'getServiceNamesFromInterface')
    // mrl.sendTo(runtimeName, 'getServiceNamesFromInterface', 'org.myrobotlab.service.interfaces.ServoController')
}
])
