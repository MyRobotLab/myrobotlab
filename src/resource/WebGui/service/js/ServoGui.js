angular.module('mrlapp.service.ServoGui', [])
.controller('ServoGuiCtrl', ['$log', '$scope', 'mrl', function($log, $scope, mrl) {
    $log.info('ServoGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    // init
    // my controllers name
    $scope.controllerName = 'HALLO';
    $scope.possibleController = 'DOH';
    // set of possible controllers
    $scope.controllers = [];
    $scope.isControllerSet = '';
    $scope.pinsList = [];
    $scope.pin = '';
    $scope.min = 0;
    $scope.max = 180;
    $scope.angle = 0;
    // $scope.email_notify_pref = { unit: 'hours', num: 1 };
    $scope.email_notify_pref = 3600;

    $scope.selection = 'oldController';

    $scope.selectedController = {
        controllerName:null
    };
    
    $scope.onSelectItem = function(item, model, label) {
        console.log(item);
    };
    
    $scope.pinList = [];
    
    //control 
    //Slider config with callbacks
    $scope.pos = {
        value: 0,
        options: {
            floor: 0,
            ceil: 180,
            onStart: function() {
            },
            onChange: function() {
                msg.send('moveTo', $scope.pos.value);
            },
            onEnd: function() {
            }
        }
    };

    //status 
    //Slider config with callbacks
    $scope.posStatus = {
        value: 0,
        options: {
            floor: 0,
            ceil: 180,
            getSelectionBarColor: "black",
            readOnly: true,
            onStart: function() {
            },
            onChange: function() {
                // msg.send('moveTo', $scope.pos.value);
            },
            onEnd: function() {
            }
        }
    };
    
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        if (service.targetPos == null){
            $scope.pos.value = service.rest;
            $scope.posStatus.value = service.rest;
        } else {
            $scope.pos.value = service.targetPos;
            $scope.posStatus.value = service.targetPos;
        }     
        
        
        $scope.controllerName = service.controllerName;
        $scope.speed = service.speed;
        $scope.isAttached = service.isAttached;
        $scope.isControllerSet = service.isControllerSet;
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
        case 'onServiceNamesFromInterface':
            $scope.controllers = data;
            $scope.$apply();
            break;
        default:
            $log.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method);
            break;
        }
        ;
    
    }
    ;
    
    $scope.update = function(speed, rest, min, max) {
        msg.send("setSpeed", speed);
        msg.send("setRest", rest);
        msg.send("setMinMax", min, max);
    }
    ;
    
    $scope.setControllerName = function(name) {
        $scope.controllerName = name;
    }
    
    $scope.setPin = function(inPin) {
        $scope.pin = inPin;
    }

    // regrettably the onMethodMap dynamic
    // generation of methods failed on this overloaded
    // sweep method - there are several overloads in the
    // Java service - although msg.sweep() was tried for ng-click
    // for some reason Js resolved msg.sweep(null, null, null, null) :P
    $scope.sweep = function() {
        msg.send('sweep');
    }

    $scope.setSelectedController=function(name){
        $log.info('setSelectedController - ' + name);
    }

    msg.subscribe("publishServoEvent");
    msg.subscribe(this);
    
    var runtimeName = mrl.getRuntime().name;
    mrl.subscribe(runtimeName, 'getServiceNamesFromInterface');
    mrl.subscribeToServiceMethod(this.onMsg, runtimeName, 'getServiceNamesFromInterface');
    mrl.sendTo(runtimeName, 'getServiceNamesFromInterface', 'org.myrobotlab.service.interfaces.ServoController');
}
]);
