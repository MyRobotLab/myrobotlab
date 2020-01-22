angular.module('mrlapp.service.ServoGui', []).controller('ServoGuiCtrl', ['$log', '$timeout', '$scope', 'mrl', function($log, $timeout, $scope, mrl) {
    $log.info('ServoGuiCtrl')
    var _self = this
    var msg = this.msg

    var firstTime = true

    // init
    $scope.controller = null
    $scope.pinsList = []
    $scope.pin = null
    $scope.min = 0
    $scope.max = 180

    $scope.possibleController = null
    $scope.testTime = 300
    $scope.sliderEnabled = false

    $scope.speed = null

    // mode is either "status" or "control"
    // in status mode we take updates by the servo and its events
    // in control mode we take updates by the user
    // $scope.mode = "status"; now statusControlMode

    // TODO - should be able to build this based on
    // current selection of controller
    $scope.pinList = []
    //slider config with callbacks
    $scope.pos = {
        value: 90,
        options: {
            floor: 0,
            ceil: 180,
            minLimit: 0,
            maxLimit: 180,
            onStart: function() {},
            onChange: function() {
                if ($scope.sliderEnabled) {
                    msg.send('moveTo', $scope.pos.value)
                }
            },
            onEnd: function() {}
        }
    }

    $scope.limits = {
        minValue: 0,
        maxValue: 180,
        options: {
            floor: 0,
            ceil: 180,
            step: 1,
            showTicks: false,
            onStart: function() {},
            /* - changing only on mouse up event - look in ServoGui.html - cannot do this !!! - sliding to the end an letting go doesnt do what you expect */
            onChange: function() {
                $scope.setMinMax()
                //msg.send('setMinMax', $scope.limits.minValue, $scope.limits.maxValue)
            },
            onEnd: function() {}
        }
    }

    $scope.setMinMax = function() {
        if ($scope.statusControlMode == 'control') {
            msg.send('setMinMax', $scope.limits.minValue, $scope.limits.maxValue)
        }
    }

    $scope.setSpeed = function(speed) {
        if (speed == null || ((typeof speed == 'string') && (speed.trim().length == 0))) {
            msg.send("unsetSpeed")
        } else {
            msg.send("setSpeed", speed)
        }
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        if (service.targetPos == null) {// $scope.pos.value = service.rest
        } else {// $scope.pos.value = service.targetPos            
        }

        if (service.controller != null) {
            $scope.possibleController = service.controller
        }
        $scope.controller = service.controller
        $scope.speed = service.speed
        $scope.pin = service.pin
        $scope.rest = service.rest

        if (firstTime) {
            $scope.pos.value = service.currentPos
            $scope.sliderEnabled = true
            firstTime = false
        }

        // set min/max mapper slider BAD IDEA !!!! control "OR" status NEVER BOTH !!!!
        $scope.limits.minValue = service.mapper.minIn
        $scope.limits.maxValue = service.mapper.maxIn
        $scope.pinList = service.pinList
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
            // servo event in the past 
            // meant feedback from MRLComm.c
            // but perhaps its come to mean
            // feedback from the service.moveTo
        case 'onServoData':
            if ($scope.statusControlMode == 'status') {
                $scope.service.currentPos = data.pos
                $scope.$apply()
            }
            break
        case 'onStatus':
            $scope.status = data
            $scope.$apply()
            break
        case 'addListener':
            // wtf?
            $log.info("Add listener called")
            $scope.status = data
            $scope.$apply()
            break
        case 'onMoveTo':
            // FIXME - whole servo is sent ? - maybe not a bad thing, but there should probably be more
            // granularity and selectiveness on what data is published when ...

            break
        default:
            $log.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    }

    $scope.getSelectionBarColor = function() {
        return "black"
    }

    $scope.isAttached = function() {
        return $scope.service.controller != null
    }

    $scope.update = function(speed, rest, min, max) {
        msg.send("setSpeed", speed)
        msg.send("setRest", rest)
        msg.send("setMinMax", min, max)
    }

    $scope.setPin = function(inPin) {
        $scope.pin = inPin
    }

    // regrettably the onMethodMap dynamic
    // generation of methods failed on this overloaded
    // sweep method - there are several overloads in the
    // Java service - although msg.sweep() was tried for ng-click
    // for some reason Js resolved msg.sweep(null, null, null, null) :P
    $scope.sweep = function() {
        msg.send('sweep')
    }
    $scope.setSelectedController = function(name) {
        $log.info('setSelectedController - ' + name)
        $scope.selectedController = name
        $scope.controller = name
    }
    $scope.attachController = function() {
        $log.info("attachController")

        // FIXME - there needs to be some updates to handle the complexity of taking updates from the servo vs
        // taking updates from the UI ..  some of this would be clearly solved with a (control/status) button

        let pos = $scope.pos.value;
        // currently taken from the slider's value :P - not good if the slider's value is not good :(

        msg.send('attach', $scope.possibleController, $scope.pin, pos)
        // $scope.rest) <-- previously used rest which is (not good)
        // msg.attach($scope.controller, $scope.pin, 90)
    }

    // msg.subscribe("publishMoveTo")
    msg.subscribe("publishServoData")
    msg.subscribe(this)

    // no longer needed - interfaces now travel with a service
    // var runtimeName = mrl.getRuntime().name
    // mrl.subscribe(runtimeName, 'getServiceNamesFromInterface')
    // mrl.subscribeToServiceMethod(this.onMsg, runtimeName, 'getServiceNamesFromInterface')
    // mrl.sendTo(runtimeName, 'getServiceNamesFromInterface', 'org.myrobotlab.service.interfaces.ServoController')
}
])
