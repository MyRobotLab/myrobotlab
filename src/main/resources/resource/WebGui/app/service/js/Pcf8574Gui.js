angular.module('mrlapp.service.Pcf8574Gui', []).controller('Pcf8574GuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('Pcf8574GuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.controllers = []
    $scope.service = {
        config:{
            controller: null
        }
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service

    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onPin':
            $scope.service.pinMap[data.pin].value = data.value
            $scope.$apply()
            break
        case 'onPinArray':
            for (i = 0; i < 8; ++i){
                let pinData = data[i] 
                $scope.service.pinMap[pinData.pin].value = pinData.value
            }
            $scope.service.pinMap[data.pin].value = data.value
            $scope.$apply()
            break
        case 'onPinDefinition':
            $scope.service.pinMap[data.pin] = data
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
        msg.send('setBus', $scope.service.config.bus)
    }

    $scope.setAddress = function() {
        msg.send('setAddress', $scope.service.config.address)
    }

    _self.setControllerName = function() {// $scope.service.config.controller = controller
    // msg.send('attach', $scope.service.config.address)
    }

    $scope.options = {
        interface: 'I2CController',
        attach: _self.setControllerName,
        // callback: function...
        attachName: $scope.service.config.controller
    }

    $scope.changed = function(pinDef, toggleValue) {
        console.info('write ', pinDef.pin, toggleValue)
        if (toggleValue) {
            msg.send('write', pinDef.pin, 1)
        } else {
            msg.send('write', pinDef.pin, 0)

        }

    }

    $scope.enablePin = function(pin, enable) {
        if (enable) {
            msg.send('enablePin', pin)
        } else {
            msg.send('disablePin', pin)
        }
            }

    $scope.attach = function() {
        if ($scope.options.attachName){
            msg.send('attach', $scope.options.attachName)    
        } else {
            msg.send('warn', 'you must select an I2C controller to attach')
        }
        
    }

    // FIXME - which i could get rid of this
    // makes attach directive worky on first load

    // regrettably the onMethodMap dynamic
    // generation of methods failed on this overloaded
    // sweep method - there are several overloads in the
    // Java service - although msg.sweep() was tried for ng-click
    // for some reason Js resolved msg.sweep(null, null, null, null) :P


    
    msg.subscribe('publishPin')
    msg.subscribe('publishPinArray')
    msg.subscribe('publishPinDefinition')
    msg.subscribe(this)
}
])
