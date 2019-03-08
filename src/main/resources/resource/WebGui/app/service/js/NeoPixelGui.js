angular.module('mrlapp.service.NeoPixelGui', [])
.controller('NeoPixelGuiCtrl', ['$log', '$scope', 'mrl', function($log, $scope, mrl) {
    $log.info('NeoPixelGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    // init
    //$scope.controller = '';
    $scope.controllerName = '';
    $scope.controllers = [];
    $scope.pins = [];
    $scope.numPixels = [];
    $scope.animations = [];
    $scope.animationData = {
      "animation": '',
      "red": 0,
      "green": 0,
      "blue": 0,
      "speed": 1
    };
      
    for (i = 0; i < 70; ++i) {
        $scope.pins.push(i);
    }
    for (i = 1; i <24 ; i++){
    	$scope.numPixels.push(i);
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        $scope.controllerName = service.controllerName;
        $scope.isAttached = service.isAttached;
        $scope.pin = service.pin;
        $scope.numPixel = service.numPixel;
        $scope.controllers = service.controllers;
        $scope.pixels = service.savedPixelMatrix;
        $scope.off = service.off;
        $scope.animations = service.animations;
        $scope.animation = service.animation;
	    $scope.animationSettingColor = service.animationSettingColor;
	    $scope.animationSettingSpeed = service.animationSettingSpeed;
     }
    ;
    
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
        case 'onStatus':
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
    
    $scope.setControllerName = function(name) {
        $scope.controllerName = name;
    }
    
    $scope.setPin = function(inPin) {
        $scope.pin = inPin;
    }

    $scope.setPixel = function(inPixel) {
        $scope.numPixel = inPixel;
    }
    
    $scope.setAnimationSetting = function(animation) {
    	$scope.animationData.animation = animation;
    	msg.send('setAnimationSetting',animation);
    }

    // regrettably the onMethodMap dynamic
    // generation of methods failed on this overloaded
    // sweep method - there are several overloads in the
    // Java service - although msg.sweep() was tried for ng-click
    // for some reason Js resolved msg.sweep(null, null, null, null) :P

    msg.subscribe(this);
    
    var runtimeName = mrl.getRuntime().name;
    mrl.subscribe(runtimeName, 'getServiceNamesFromInterface');
    mrl.subscribeToServiceMethod(this.onMsg, runtimeName, 'getServiceNamesFromInterface');
    mrl.sendTo(runtimeName, 'getServiceNamesFromInterface', 'org.myrobotlab.service.interfaces.NeoPixelController');
}
]);
