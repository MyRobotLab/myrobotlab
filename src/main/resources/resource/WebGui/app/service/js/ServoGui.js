angular.module('mrlapp.service.ServoGui', []).controller('ServoGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('ServoGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.disableServicePosUpdates = false

    $scope.minView = true

    // mode is either "status" or "control"
    // in status mode we take updates by the servo and its events
    // in control mode we take updates by the user
    // $scope.mode = "status"; now statusControlMode

    // useful for initialization of components
    var firstTime = true

    $scope.state = {
        useEncoderData: false,
        showLimits: false,
        rest: 90
    }

    $scope.attachController = function(serviceName) {
        console.info("attachController")
        if ($scope.service.pin){
            msg.send("setPin", $scope.service.pin)            
        }
        if ($scope.service.config.controller){
            msg.send("attach", $scope.service.config.controller)
        }
        msg.send("broadcastState")
    }

    $scope.setController = function(serviceName) {
        console.info("setController")
        msg.send("setController", serviceName)
    }
    
    $scope.servoOptions = {
        interface: 'ServoController',
        isAttached: $scope.service.isAttached,
        // callback: function...
        attach: $scope.setController,
        attachName: $scope.service?.config?.controller,
        controllerTitle: 'controller'
    }


    // init
    $scope.min = 0
    $scope.max = 180

    $scope.possibleControllers = null
    $scope.testTime = 300
    $scope.sliderEnabled = false
    $scope.speedDisplay = '0'
    $scope.idleSeconds = 0

    $scope.speed = null
    $scope.lockInputOutput = false

    $scope.activeTabIndex = 1

    $scope.optionsWithoutStart = {
        connect: true,
        range: {
            min: 0,
            max: 100,
        },
    };

    $scope.sliderPositions = [20, 80];

    // TODO - should be able to build this based on
    // current selection of controller
    $scope.pinList = []
    for (let i = 0; i < 58; ++i) {
        $scope.pinList.push(i + '')
        // make strings 
    }

    $scope.setRest = function() {
        msg.send('setRest', $scope.service.rest)
    }

    $scope.toggleLock = function() {
        $scope.lockInputOutput = !$scope.lockInputOutput
    }

    $scope.setMinMax = function(min, max) {
        console.log('setMinMax ', min, max)
        msg.send('setMinMax', min, max)
        /*
        if ($scope.statusControlMode == 'control') {
            msg.send('setMinMax', $scope.limits.minValue, $scope.limits.maxValue)
        }
        */
    }

    $scope.setSpeed = function() {
        if ($scope.service.speed == null || $scope.service.speed == "201") {
            // if speed is null or speed is "max slider value" unset the speed
            msg.send("unsetSpeed")
        } else {
            msg.send("setSpeed", $scope.service.speed)
        }
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {

        $scope.service = service

        // done correctly - speedDisplay is a 'status' display !
        // its NOT used to set 'control' speed - control is sent
        // from the ui interface - but the ui component does not display what it sent
        // speedDisplay displays what was recieved - and is currently set
        if (service.speed) {
            $scope.speedDisplay = service.speed.toFixed(0)
        } else {
            $scope.speedDisplay = 'Max'
            service.speed = 201
            // max range of slider bar
        }

        if (service?.config?.controller){
            $scope.servoOptions.attachName = service?.config?.controller
        }

        $scope.idleSeconds = service.idleTimeout / 1000
        //         $scope.pos.options.minLimit = service.mapper.minX
        //         $scope.pos.options.maxLimit = service.mapper.maxX

        // ui initialization - good idea !
        // first time is 'status' - otherwise control
        if (firstTime) {

            // control assigned = status for "non" broadcastState methods
            // guarded by firstTime - it never will get  updated after
            // initialization
            $scope.state.rest = service.rest

            // $scope.pos.value = service.currentInputPos
            $scope.sliderEnabled = true

            $scope.activeTabIndex = service.isAttached?1:0

            $scope.state.inputMin = service.mapper.minX
            $scope.state.inputMax = service.mapper.maxX
            $scope.state.outputMin = service.mapper.minY
            $scope.state.outputMax = service.mapper.maxY

            firstTime = false
        }

        // set min/max mapper slider BAD IDEA !!!! control "OR" status NEVER BOTH !!!!
        // $scope.pinList = service.pinList
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onServoSetSpeed':
            if (data.speed) {
                $scope.speedDisplay = data.speed
            } else {
                $scope.speedDisplay = 'Max'
            }
            $scope.$apply()
            break
        case 'onRefreshControllers':
            $scope.possibleControllers = data
            $scope.$apply()
            break
        case 'onEncoderData':
            $scope.service.currentInputPos = Math.floor(data.mappedValue)
            $scope.$apply()
            break
        case 'onServoMoveTo':
            if (!$scope.disableServicePosUpdates) {
                // if the user is not controlling the slider - let the servo service
                if (data.inputPos) {
                    $scope.service.targetPos = data.inputPos.toFixed(0)
                    $scope.$apply()

                }
            }
            break
        case 'onServoEnable':
            $scope.service.enabled = true
            $scope.$apply()
            break
        case 'onServoDisable':
            $scope.service.enabled = false
            $scope.$apply()
            break
        case 'onServoStopped':
            //$scope.service.currentInputPos = data.pos
            $scope.$apply()
            break
        case 'onServoStarted':
            // $scope.service.currentInputPos = data.pos
            // $scope.$apply()
            break
        case 'onStatus':
            break
        default:
            console.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }
    }

    $scope.toggleEncoderData = function() {
        if ($scope.state.useEncoderData) {
            msg.subscribe("publishEncoderData")
            msg.send('broadcastState')
        } else {
            msg.unsubscribe("publishEncoderData")
        }
    }

    $scope.setPin = function() {
        msg.send('setPin', $scope.service.pin)
    }

    $scope.setAutoDisable = function() {
        msg.send("setIdleTimeout", $scope.service.idleTimeout)
        msg.send("setAutoDisable", $scope.service.config.autoDisable)
    }

    $scope.sweep = function() {
        msg.send('sweep')
    }

    $scope.moveTo = function(pos) {
        msg.send('moveTo', pos)
    }

    $scope.setIdleTimeout = function(idleTime) {
        msg.send('setIdleTimeout', idleTime * 1000)
    }

    $scope.toggleServoSpeedBar = function (service) {
      service.speedBar = !service.speedBar
    }
    
    $scope.map = function() {

        if ($scope.lockInputOutput) {
            $scope.service.mapper.minY = $scope.service.mapper.minX
            $scope.service.mapper.maxY = $scope.service.mapper.maxX
        }

        msg.send('map', $scope.service.mapper.minX, $scope.service.mapper.maxX, $scope.service.mapper.minY, $scope.service.mapper.maxY)
        msg.send("broadcastState")
    }

    $scope.disableUpdates = function() {
        $scope.disableServicePosUpdates = true
    }
    $scope.enableUpdates = function() {
        $scope.disableServicePosUpdates = false
    }

    // msg.subscribe("publishMoveTo") - can cause a infinite loopback control-> status
    // msg.subscribe("publishEncoderData") - not a good idea will swamp with data
    msg.subscribe("publishServoEnable")
    msg.subscribe("publishServoDisable")
    msg.subscribe("publishServoStopped")
    // msg.subscribe("publishServoStarted")
    msg.subscribe("publishServoMoveTo")
    msg.subscribe("publishServoSetSpeed")
    msg.subscribe("refreshControllers")
    msg.subscribe(this)
    msg.send('refreshControllers')
}
])
