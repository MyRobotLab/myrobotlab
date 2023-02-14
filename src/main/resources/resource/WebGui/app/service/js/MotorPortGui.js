angular.module('mrlapp.service.MotorPortGui', []).controller('MotorPortGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('MotorPortGuiCtrl')
    var _self = this
    var msg = this.msg
    var firstTime = true

    $scope.requestedPower = 0
    $scope.powerOutput = 0

    $scope.powerSlider = {
        value: 0,
        options: {
            floor: -100,
            ceil: 100,
            minLimit: -100,
            maxLimit: 100,
            // hideLimitLabels: true,
            onStart: function() {},
            onChange: function() {
                // if ($scope.sliderEnabled) {
                msg.send('move', $scope.requestedPower / 100)
                //}
            },
            onEnd: function() {}
        }
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        if (firstTime) {
            $scope.requestedController = service.config.controller
            firstTime = false
        }
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {

        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break

        case 'onRefreshControllers':
            $scope.service.controllers = data
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
        msg.send('map', $scope.service.config.mapper.minIn, $scope.service.config.mapper.maxIn, $scope.service.config.mapper.minOut, $scope.service.config.mapper.maxOut)
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
        console.info('detach', $scope.service.config.controller)
        msg.send('detach', $scope.service.config.controller)
    }

    $scope.moveTo = function() {
        console.info('moveTo')
        msg.send('moveTo', $scope.moveToPos)
    }

    
    $scope.setSpeed = function() {
        msg.send('move', $scope.requestedPower/100)
        // console.info('move', $scope.requestedPower/100)
    }
    
    msg.subscribe("refreshControllers")
    msg.subscribe("publishPowerChange")
    msg.subscribe("publishPowerOutputChange")
    msg.send("refreshControllers")
    msg.subscribe(this)
}
])
