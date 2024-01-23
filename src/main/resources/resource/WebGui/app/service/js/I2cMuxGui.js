angular.module("mrlapp.service.I2cMuxGui", []).controller("I2cMuxGuiCtrl", ["$scope", "mrl", function($scope, mrl) {
    console.info("I2cMuxGuiCtrl")
    var _self = this
    var msg = this.msg

    _self.selectController = function(controller) {
        //$scope.service.config.controller = controller
        $scope.service.config.controller = controller
        // get the pin list of the selected controller
        // msg.send("setPinArrayControl", controller)
        // msg.send("getPinList", controller)
    }

    $scope.options = {
        interface: "I2CController",
        attach: _self.selectController,
        // isAttached: $scope.service.config.controller // callback: function...
        // attachName: $scope.config.controller
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.options.attachName = service.config.controller

        // since attach broadcasts we'll get the pin list here
        if ($scope?.service?.config?.controller) {
            msg.send("getPinList", $scope.service.config.controller)
        }
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case "onState":
            _self.updateState(data)
            $scope.$apply()
            break
        default:
            console.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }
    }

    $scope.setAddress = function(address) {
        msg.send('setAddress', address)
        msg.send('broadcastState')
    }

    $scope.setBus = function(bus) {
        msg.send('setBus', bus)
        msg.send('broadcastState')
    }
    
    $scope.attach = function() {
        msg.send('setBus', $scope.service.config.bus)
        msg.send('setAddress', $scope.service.config.address)
        msg.send('attach', $scope.config.controller)
    }

    $scope.detach = function() {
        if ($scope.service.config.controller) {
            msg.send('detach', $scope.service.config.controller)
        }
    }

    $scope.setControllerName = function(name) {
        $scope.config.controller = name
    }

    $scope.setDeviceBus = function(bus) {
        $scope.deviceBus = bus
    }

    $scope.setDeviceAddress = function(address) {
        $scope.deviceAddress = address
    }

    // regrettably the onMethodMap dynamic
    // generation of methods failed on this overloaded
    // sweep method - there are several overloads in the
    // Java service - although msg.sweep() was tried for ng-click
    // for some reason Js resolved msg.sweep(null, null, null, null) :P

    msg.subscribe(this)
}
, ])
