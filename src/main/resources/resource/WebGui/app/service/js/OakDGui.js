angular.module('mrlapp.service.OakDGui', []).controller('OakDGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('OakDGuiCtrl')
    var _self = this
    var msg = this.msg

    this.updateState = function(service) {
        $scope.service = service
        if (service && service.lastClick) {
            $scope.click = service.lastClick
        }
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onClassification':
              $scope.classification = data
              $scope.latency = Date.now() - data.ts
              $scope.$apply()             
            break
        case 'onImageToWeb':
              $scope.image = data
              $scope.$apply()
            break
        case 'onWebDisplay':
              $scope.preview = data
              _self.notePreviewFps()
              $scope.$apply()
            break
        case 'onDepthClick':
              $scope.click = data
              $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.preview = null
    $scope.stats = {
        fps: 0
    }
    var fpsCount = 0
    var fpsWindowStart = 0

    this.notePreviewFps = function() {
        var now = Date.now()
        if (!fpsWindowStart) {
            fpsWindowStart = now
        }
        fpsCount++
        var elapsed = now - fpsWindowStart
        if (elapsed >= 500) {
            $scope.stats.fps = Math.round(fpsCount * 1000 / elapsed)
            fpsCount = 0
            fpsWindowStart = now
        }
    }

    $scope.startDepth = function() {
        msg.send('startDepth')
    }
    $scope.stopDepth = function() {
        msg.send('stopDepth')
    }
    $scope.startSynthetic = function() {
        msg.send('startSyntheticDepth')
    }
    $scope.knownDistanceM = 1

    $scope.setRgbMesh = function() {
        msg.send('setRgbMesh', $scope.service.config.rgbMesh)
    }

    $scope.setDisplayWeb = function() {
        msg.send('setDisplayWeb', $scope.service.config.displayWeb)
    }

    $scope.setDepthCloudScale = function() {
        msg.send('setDepthCloudScale', parseFloat($scope.service.depthCloudScale))
    }

    $scope.resetDepthCloudScale = function() {
        $scope.service.depthCloudScale = 1
        msg.send('setDepthCloudScale', 1)
    }

    $scope.calibrateDepthScale = function() {
        msg.send('calibrateDepthScale', parseFloat($scope.knownDistanceM))
    }

    $scope.clickView = function(event) {
        var img = event.currentTarget
        if (!img || !img.clientWidth || !img.clientHeight) {
            return
        }
        msg.send('clickImage', event.offsetX / img.clientWidth, event.offsetY / img.clientHeight)
    }

    // Padding gives the image box a real height, so the marker's top % lines up.
    $scope.previewBox = function() {
        var w = $scope.preview && $scope.preview.width
        var h = $scope.preview && $scope.preview.height
        var ratio = (w && h) ? (h / w * 100) : 75
        return { 'padding-bottom': ratio + '%' }
    }

    $scope.clickMarker = function() {
        if (!$scope.click) {
            return {}
        }
        var nx = Math.min(1, Math.max(0, $scope.click.nx))
        var ny = Math.min(1, Math.max(0, $scope.click.ny))
        return {
            left: (nx * 100) + '%',
            top: (ny * 100) + '%'
        }
    }

    msg.subscribe('publishClassification')
    msg.subscribe('imageToWeb')
    msg.subscribe('publishWebDisplay')
    msg.subscribe('publishDepthClick')
    msg.subscribe(this)
}
])
