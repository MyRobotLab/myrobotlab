angular.module('mrlapp.service.OpenCVGui', []).controller('OpenCVGuiCtrl', ['$scope', 'mrl', '$uibModal', function($scope, mrl, $uibModal) {
    console.info('OpenCVGuiCtrl')
    // grab a reference
    var _self = this
    // grab the message
    var msg = this.msg

    var addFilterDialog = null

    $scope.fps = 0

    $scope.myFile = null

    $scope.lastFrameTs = null

    $scope.stats = {
        latency: 0,
        fps: 0
    }

    $scope.samplePoint = {
        x: 0,
        y: 0
    }

    var avgSampleCnt = 30

    var latencyDeltaAccumulator = 0

    var lastFrameIndex = 0

    var lastFrameTs = 0

    /**
     * Filter "Types" - this is the meta data type information necessary to build
     * filter dialogs which get and set the filter attributes.
     * in OpenCV.html - you can find were these are used when getFilterType() is called
     */
    $scope.filterMetaData = {
        'AdaptiveThreshold': {
            algorithm: 'mean'
        },
        'Affine': {
        },

        'Canny': {
        },
        'LKOpticalTrack': {
        }// LKOpticalTrack

    }

    $scope.diplayImage = null

    // first in list
    $scope.selectedFilterType = 'AdaptiveThreshold'
    // $scope.displayFilter = null

    // local scope variables
    // necessary because service.cameraIndex is an int but ng-option only handles strings
    // $scope.cameraIndex = "0"
    $scope.camera = {
        index: "0"
    }

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
        console.info("Open CV State had been updated")
        console.info(service)
        // int to string conversion
        $scope.camera.index = service.cameraIndex.toString()
        if ($scope.service.capturing) {
            console.info("Started capturing")
            $scope.startCaptureLabel = "Stop Capture"
            $scope.imgSource = "http://localhost:9090/input"
        } else {
            console.info("Stopped capturing.")
            $scope.startCaptureLabel = "Start Capture"
            $scope.imgSource = "service/img/OpenCV.png"
        }

    }

    $scope.addFilter = function(size) {

        addFilterDialog = $uibModal.open({
            templateUrl: "addFilterDialog.html",
            scope: $scope,
            controller: function($scope) {
                $scope.cancel = function() {
                    addFilterDialog.dismiss()
                }
            }
        })
    }

    $scope.addNamedFilter = function(name) {
        console.info('addNamedFilter', name, $scope.selectedFilterType)
        msg.send('addFilter', name, $scope.selectedFilterType)
        if (addFilterDialog) {
            addFilterDialog.dismiss()
        }
    }

    $scope.setDisplayFilter = function(name) {
        console.info('setDisplayFilter', name)
        msg.send('setDisplayFilter', name)
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
        case 'onWebDisplay':
            // $scope.diplayImage = 'data:image/jpeg;base64,' + data
            $scope.diplayImage = data.data
            if (data.frameIndex % avgSampleCnt == 0) {
                $scope.stats.latency = Math.round(latencyDeltaAccumulator / avgSampleCnt)
                latencyDeltaAccumulator = 0
                $scope.stats.fps = Math.round((data.frameIndex - lastFrameIndex) * 1000 / (data.ts - lastFrameTs))
                lastFrameIndex = data.frameIndex
                lastFrameTs = data.ts
            }

            latencyDeltaAccumulator += new Date().getTime() - data.ts

            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    // FIXME - rename isFitlerType('Canny')
    $scope.isFilterType = function(type) {
        if ($scope.service.filters[$scope.service.displayFilter]) {
            return $scope.service.filters[$scope.service.displayFilter].type == type
        }
        return null
    }

    // get currently selected filter
    $scope.getFilter = function() {
        if ($scope.service.filters[$scope.service.displayFilter]) {
            return $scope.service.filters[$scope.service.displayFilter]
        }
        return null
    }

    $scope.getDisplayImage = function() {
        return $scope.diplayImage
    }

    $scope.setFilterState = function() {
        let filter = $scope.service.filters[$scope.service.displayFilter]
        // let meta = $scope.getFilterType().apertureSize.options
        // let x = $scope.service.filters[$scope.service.displayFilter].apertureSize
        msg.send('setFilterState', filter.name, JSON.stringify(filter))
        console.info(filter)
    }

    $scope.getFilterType = function(typeName) {
        if (!typeName) {
            typeName = $scope.service.displayFilter
        }
        if ($scope.service.filters[typeName]) {
            return $scope.filterMetaData[$scope.service.filters[$scope.service.displayFilter].type]
        }
        return null
    }

    $scope.meta = function() {
        let type = $scope.isFilterType()
    }

    $scope.onSamplePoint = function($event) {
        console.info('samplePoint ' + $event)
        $scope.samplePoint.x = $event.offsetX
        $scope.samplePoint.y = $event.offsetY
        msg.send('samplePoint', $scope.samplePoint.x, $scope.samplePoint.y)
    }

    $scope.uploadFile = function() {

        var f = $scope.myFile;
        var r = new FileReader();

        r.onloadend = function(e) {
            var data = e.target.result;
            console.info('onloadend')
            msg.send('saveFile', f.name, btoa(data))
            $scope.loadFile = false
            // close dialog
        }

        r.readAsBinaryString(f);
        console.info('readAsBinaryString')
    }

    msg.subscribe('getPossibleFilters')
    msg.subscribe('publishWebDisplay')
    msg.subscribe('publishState')
    msg.send('getPossibleFilters')
    msg.subscribe(this)

}]).directive('fileModel', ['$parse', function($parse) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            var model = $parse(attrs.fileModel);
            var modelSetter = model.assign;

            element.bind('change', function() {
                scope.$apply(function() {
                    modelSetter(scope, element[0].files[0]);
                });
            });
        }
    };
}
]);
