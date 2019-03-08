angular.module('mrlapp.service.ServoGui', []).controller('ServoGuiCtrl', ['$log', '$scope', 'mrl', function($log, $scope, mrl) {
    $log.info('ServoGuiCtrl');
    var _self = this;
    var msg = this.msg;
    // init
    $scope.controllerName = null ;
    $scope.pinsList = [];
    $scope.pin = null ;
    $scope.min = 0;
    $scope.max = 180;
    $scope.possibleController = null;
    $scope.testTime = 300;
    // TODO - should be able to build this based on
    // current selection of controller
    $scope.pinList = [];
    //slider config with callbacks
    $scope.pos = {
        value: 0,
        options: {
            floor: 0,
            ceil: 180,
            onStart: function() {},
            onChange: function() {
                msg.send('moveTo', $scope.pos.value);
            },
            onEnd: function() {}
        }
    };
    //status 
    //Slider config with callbacks
    $scope.posStatus = {
        value: 0,
        options: {
            floor: 0,
            ceil: 180,
            // getSelectionBarColor: "black",
            readOnly: true,
            onStart: function() {},
            onChange: function() {// msg.send('moveTo', $scope.pos.value);
            },
            onEnd: function() {}
        }
    };
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        if (service.targetPos == null ) {
            $scope.pos.value = service.rest;
            $scope.posStatus.value = service.rest;
        } else {
            $scope.pos.value = service.targetPos;
            $scope.posStatus.value = service.targetPos;
        }
        $scope.possibleController = service.controllerName;
        $scope.controllerName = service.controllerName;
        $scope.velocity = service.velocity;
        $scope.pin = service.pin;
        $scope.rest = service.rest;
        $scope.min = service.mapper.minOutput;
        $scope.max = service.mapper.maxOutput;
        $scope.pinList = service.pinList;
    }
    ;
    // initialize our state
    _self.updateState($scope.service);
    this.onMsg = function(inMsg) {
        var data = inMsg.data[0];
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data);
            $scope.$apply();
            break;
            // servo event in the past 
            // meant feedback from MRLComm.c
            // but perhaps its come to mean
            // feedback from the service.moveTo
        case 'onServoEvent':
            $scope.posStatus.value = data;
            $scope.$apply();
            break;
        case 'onStatus':
            $scope.status = data;
            $scope.$apply();
            break;
        case 'addListener':
            // wtf?
            $log.info("Add listener called");
            $scope.status = data;
            $scope.$apply();
            break;
        default:
            $log.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method);
            break;
        }
        ;
    }
    ;
    $scope.getSelectionBarColor = function(){
        return "black";
    };
    $scope.isAttached = function() {
        return $scope.service.controllerName != null ;
    }
    ;
    $scope.update = function(velocity, rest, min, max) {
        msg.send("setVelocity", velocity);
        msg.send("setRest", rest);
        msg.send("setMinMax", min, max);
    }
    ;
    $scope.setPin = function(inPin) {
        $scope.pin = inPin;
    }
    ;
    // regrettably the onMethodMap dynamic
    // generation of methods failed on this overloaded
    // sweep method - there are several overloads in the
    // Java service - although msg.sweep() was tried for ng-click
    // for some reason Js resolved msg.sweep(null, null, null, null) :P
    $scope.sweep = function() {
        msg.send('sweep');
    }
    $scope.setSelectedController = function(name) {
        $log.info('setSelectedController - ' + name);
        $scope.selectedController = name;
        $scope.controllerName = name;
    }
    $scope.attachController = function() {
        $log.info("attachController");
        msg.send('attach', $scope.possibleController, $scope.pin, $scope.rest);
        // msg.attach($scope.controllerName, $scope.pin, 90);
    }
    msg.subscribe("publishServoEvent");
    msg.subscribe(this);
    // no longer needed - interfaces now travel with a service
    // var runtimeName = mrl.getRuntime().name;
    // mrl.subscribe(runtimeName, 'getServiceNamesFromInterface');
    // mrl.subscribeToServiceMethod(this.onMsg, runtimeName, 'getServiceNamesFromInterface');
    // mrl.sendTo(runtimeName, 'getServiceNamesFromInterface', 'org.myrobotlab.service.interfaces.ServoController');
}
]);
