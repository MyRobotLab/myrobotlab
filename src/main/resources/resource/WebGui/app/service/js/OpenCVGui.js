angular.module('mrlapp.service.OpenCVGui', []).controller('OpenCVGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl, $sce) {
    $log.info('OpenCVGuiCtrl')
    // grab a refernce
    var _self = this
    // grab the message
    var msg = this.msg

    // local scope variables
    $scope.cameraIndex = 1
    $scope.frameGrabber = "VideoInput"
    $scope.possibleFilters = null

    // initial state of service.

    if ($scope.service.capturing) {
        $scope.startCaptureLabel = "Stop Capture"
        // $sce.trustAsResourceUrl ?
        $scope.imgSource = "http://localhost:9090/input"
    } else {
        $scope.startCaptureLabel = "Start Capture"
        $scope.imgSource = "service/img/opencv.png"
    }

    // Handle an update state call from OpenCV service.
    this.updateState = function(service) {
        $scope.service = service
        $log.info("Open CV State had been updated")
        $log.info(service)
        if ($scope.service.capturing) {
            $log.info("Started capturing")
            $scope.startCaptureLabel = "Stop Capture"
            $scope.imgSource = "http://localhost:9090/input"
        } else {
            $log.info("Stopped capturing.")
            $scope.startCaptureLabel = "Start Capture"
            $scope.imgSource = "service/img/OpenCV.png"
        }

    }

    // controls for select frame grabber
    $scope.selectFrameGrabber = function selectFrameGrabber(frameGrabber) {
        $log.info("Updating Frame Grabber ")
        $scope.frameGrabber = frameGrabber
        mrl.sendTo($scope.service.name, "setGrabberType", frameGrabber)
    }

    // controls for select frame grabber                
    $scope.selectCameraIndex = function(cameraIndex) {
        $log.info("Updating Camera Index ..." + cameraIndex)
        $scope.cameraIndex = cameraIndex
        mrl.sendTo($scope.service.name, "setCameraIndex", cameraIndex)
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onPossibleFilters':
            $scope.possibleFilters = data
            $scope.$apply()
            break
        case 'onDisplay':
            // $scope.pulseData = inMsg.data[0]
            $scope.$apply()
            break
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    // TODO: we're not going to publish the display.
    // we will start a video stream and update the page to display that stream.
    // mrl.subscribe($scope.service.name, 'publishState')
    msg.subscribe('getPossibleFilters')
    msg.subscribe('publishState')
    msg.send('getPossibleFilters')
    msg.subscribe(this)

}
])
