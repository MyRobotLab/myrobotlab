angular.module('mrlapp.service.MotorDualPwmGui', []).controller('MotorDualPwmGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('MotorDualPwmGuiCtrl')
    var _self = this
    var msg = this.msg
    var firstTime = true

    $scope.requestedPower = 0
    $scope.powerOutput = 0
    $scope.options = {
        interface: 'PinArrayControl',
        attach: _self.selectController,
        // isAttached: $scope.service.config.controller // callback: function...
        // attachName: $scope.controllerName
    }


    $scope.powerSlider = {
        value: 0,
        options: {
            floor: -100,
            ceil: 100,
            minLimit: -100,
            maxLimit: 100,
            onStart: function() {},
            onChange: function() {
                msg.send('move', $scope.requestedPower / 100)
            },
            onEnd: function() {}
        }
    }

    $scope.pins = []

    for(i = 0; i < 30 ; ++i){
        $scope.pins.push(i)
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        if (firstTime) {
            $scope.requestedController = service.config.controller
            firstTime = false

            $scope.options.attachName = service.config.controller
            $scope.options.isAttached = service.attached
            $scope.options.interface = 'PinArrayControl'    
        }
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {

        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break

        case 'onStatus':
            console.info('onStatus', data)
            break

        case 'onPowerChange':
            $scope.service.powerInput = data
            $scope.$apply()
            break

        case 'onPowerOutputChange':
            $scope.powerOutput = data
            $scope.$apply()
            break

        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.stop = function() {
        console.info('stop')
        $scope.requestedPower = 0
        msg.send('stop')
    }

    $scope.update = function() {
        console.info('update')
        msg.send('map', $scope.service.config.mapper.minIn, $scope.service.config.mapper.minIn, $scope.service.config.mapper.minOut, $scope.service.config.mapper.maxOut)
    }

    $scope.setController = function(c) {
        console.info('setController', c)
        $scope.requestedController = c
    }

    $scope.attach = function() {
        console.info('attach')
        msg.send('attach', $scope.requestedController)
    }

    $scope.detach = function() {
        console.info('detach')
        msg.send('detach')
    }

    $scope.moveTo = function() {
        console.info('moveTo')
        msg.send('moveTo', $scope.moveToPos)
    }

    
    $scope.setSpeed = function() {
        msg.send('setSpeed', $scope.requestedPower)
    }


    msg.subscribe("publishPowerChange")
    msg.subscribe("publishPowerOutputChange")
    msg.subscribe(this)
}
])
