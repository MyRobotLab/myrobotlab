angular.module('mrlapp.service.Pcf8574Gui', []).controller('Pcf8574GuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('Pcf8574GuiCtrl')
    var _self = this
    var msg = this.msg

    // init
    $scope.controllerName = ''
    $scope.controllers = []

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service

        for (var key in service.pinMap) {
            if (service.pinMap.hasOwnProperty(key)) {
                console.log(key, service.pinMap[key]);

                service.pinMap[key].value = 0 //service.pinDef[key].value
                //if (key == 'D3') {
                //    service.pinMap[key].value = 1
                //}
            }
        }
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onPinArray':
            // a NOOP - but necessary 
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }
    }

    $scope.setBus = function() {
        msg.send('setBus', $scope.service.deviceBus)
    }

    $scope.setAddress = function() {
        msg.send('setAddress', $scope.service.deviceAddress)
    }

    _self.setControllerName = function() {// $scope.service.controllerName = controller
    // msg.send('attach', $scope.service.deviceAddress)
    }

    $scope.options = {
        interface: 'I2CController',
        attach: _self.setControllerName,
        // callback: function...
        attachName: $scope.service.controllerName
    }

    $scope.changed = function(pinDef, toggleValue) {
        console.info($scope.toggleValue)
        if (toggleValue == false) {
            msg.send('write', pinDef.pin, 0)
        } else {
            msg.send('write', pinDef.pin, 1)

        }

    }

    $scope.attach = function() {
        msg.send('attach', $scope.options.attachName)
    }

    // FIXME - which i could get rid of this
    // makes attach directive worky on first load

    // regrettably the onMethodMap dynamic
    // generation of methods failed on this overloaded
    // sweep method - there are several overloads in the
    // Java service - although msg.sweep() was tried for ng-click
    // for some reason Js resolved msg.sweep(null, null, null, null) :P

    msg.subscribe(this)
}
])
