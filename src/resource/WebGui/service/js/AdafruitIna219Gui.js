angular.module('mrlapp.service.AdafruitIna219Gui', [])
.controller('AdafruitIna219GuiCtrl', ['$log', '$scope', 'mrl', function($log, $scope, mrl) {
    $log.info('AdafruitIna219GuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    // init
    $scope.controllerName = '';
    $scope.controllers = [];
    $scope.controllerLabel = 'Controller :';
    $scope.deviceBusLabel = 'Bus :';
    $scope.deviceAddressLabel = 'Address :';    
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        $scope.controllers = service.controllers;
        $scope.controllerName = service.controllerName;
        $scope.deviceBusList = service.deviceBusList;
        $scope.deviceBus = service.deviceBus;
        $scope.deviceAddressList = service.deviceAddressList;
        $scope.deviceAddress = service.deviceAddress;
        $scope.isAttached = service.isAttached;
        $scope.busVoltage = service.busVoltage;
        $scope.shuntVoltage = service.shuntVoltage;
        $scope.current = service.current;
        $scope.power = service.power;
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
    
    $scope.setDeviceBus = function(bus) {
        $scope.deviceBus = bus;
    }
    
    $scope.setDeviceAddress = function(address) {
        $scope.deviceAddress = address;
    }
    
    // regrettably the onMethodMap dynamic
    // generation of methods failed on this overloaded
    // sweep method - there are several overloads in the
    // Java service - although msg.sweep() was tried for ng-click
    // for some reason Js resolved msg.sweep(null, null, null, null) :P

    msg.subscribe(this);
}
]);
