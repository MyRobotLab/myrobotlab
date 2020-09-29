angular.module('mrlapp.service.WebcamGui', []).controller('WebcamGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('WebcamGuiCtrl')
    var _self = this
    var msg = this.msg

    var latencyDeltaAccumulator = 0

    var frameIndex = 0

    var lastFrameIndex = 0

    var lastFrameTs = 0

    var port = 8080

    $scope.webImage = null

    $scope.selectedCamera = null

    $scope.url = location.protocol + '//' + location.hostname + (port ? ':' + port : '')

    $scope.fps = 0

    $scope.lastFrameTs = null

    $scope.stats = {
        latency: 0,
        fps: 0,
        deltaTime: 0,
        deltaFrames: 0,
        diff: 0
    }

    $scope.samplePoint = {
        x: 0,
        y: 0
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.port = service.port
    }

    // init scope variables
    $scope.pulseData = ''

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0])
            $scope.$apply()
            break
        case 'onWebDisplay':
            $scope.webImage = data
            
            frameIndex++;
            let now = new Date().getTime()
            let deltaTime = now - lastFrameTs

            if (deltaTime > 1000) {
                // ~ 1 sec
                $scope.stats.deltaFrames = frameIndex - lastFrameIndex

                // latency
                $scope.stats.latency = Math.abs(Math.round(latencyDeltaAccumulator / deltaTime))
                latencyDeltaAccumulator = 0

                $scope.stats.fps = Math.round((frameIndex - lastFrameIndex) * 1000 / (deltaTime))
                lastFrameTs = now

                // frames lost
                $scope.stats.diff = data.frameIndex - frameIndex

                // reset
                lastFrameIndex = frameIndex;
                lastFrameTs = now

            }

            latencyDeltaAccumulator += now - data.ts
            $scope.$apply()
            break
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.onSamplePoint = function($event) {
        console.info('samplePoint ' + $event)
        $scope.samplePoint.x = $event.offsetX
        $scope.samplePoint.y = $event.offsetY
        msg.send('samplePoint', $scope.samplePoint.x, $scope.samplePoint.y)
    }

    $scope.getDisplayImage = function() {
        return $scope.webImage.data
    }

    $scope.capture = function(){
        console.info('capture', $scope.service.selectedCamera, $scope.service.type, $scope.service.requestedFps, $scope.service.width, $scope.service.height, $scope.service.quality)
        msg.send('capture', $scope.service.selectedCamera, $scope.service.type, $scope.service.requestedFps, $scope.service.width, $scope.service.height, $scope.service.quality)
    }

    //mrl.subscribe($scope.service.name, 'pulse')
    msg.subscribe('publishShowAll')
    // msg.subscribe('publishHideAll') FIXME ? not symmetric
    msg.subscribe('publishHide')
    msg.subscribe('publishShow')
    msg.subscribe('publishSet')
    msg.subscribe('publishWebDisplay')
    msg.subscribe(this)
}
])
