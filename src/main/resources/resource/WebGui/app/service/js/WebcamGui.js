angular.module('mrlapp.service.WebcamGui', []).controller('WebcamGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('WebcamGuiCtrl')
    var _self = this
    var msg = this.msg

    var avgSampleCnt = 30

    var latencyDeltaAccumulator = 0

    var lastFrameIndex = 0

    var lastFrameTs = 0

    var port = 8080
    $scope.url = location.protocol + '//' + location.hostname + (port ? ':' + port : '')

    $scope.fps = 0

    $scope.lastFrameTs = null

    $scope.stats = {
        latency: 0,
        fps: 0
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
            // $scope.diplayImage = 'data:image/jpeg;base64,' + data
            $scope.diplayImage = data.data
            if (data.frameIndex % avgSampleCnt == 0) {
                $scope.stats.latency = Math.round(latencyDeltaAccumulator / avgSampleCnt)
                latencyDeltaAccumulator = 0
                $scope.stats.fps = (data.ts - lastFrameTs)
                //$scope.stats.fps = Math.round((data.frameIndex - lastFrameIndex) * 1000 / (data.ts - lastFrameTs))
                lastFrameIndex = data.frameIndex
                lastFrameTs = data.ts
            }

            latencyDeltaAccumulator += new Date().getTime() - data.ts

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
        return $scope.diplayImage
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
