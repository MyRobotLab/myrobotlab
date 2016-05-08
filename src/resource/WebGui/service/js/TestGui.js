angular.module('mrlapp.service.TestGui', [])
.controller('TestGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', '$sce', function($scope, $log, mrl, $timeout, $sce) {
    $log.info('TestGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    // init scope variables
    $scope.matrix = {};
    $scope.currentProgress = {};
    $scope.currentProgress.percentDone = 0;
    $scope.currentProgress.currentActivity = "ready";
    $scope.servicesToTest = [];
    $scope.testsToRun = ['PythonScriptExists', 'ServicePageExists'];
    $scope.trustAsHtml = $sce.trustAsHtml;
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        $scope.matrix = service.matrix;
        $scope.currentProgress = service.matrix.currentProgress;
        $scope.servicesToTest = service.matrix.servicesToTest;
    }
    ;
    
    _self.updateState($scope.service);
    
    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0]);
            break;
        case 'onProgress':
            $scope.currentProgress = inMsg.data[0];
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
            break;
        }
    }
    ;
    
    // toggle service to test
    $scope.toggleSelection = function toggleSelection(serviceName) {
        var idx = $scope.servicesToTest.indexOf(serviceName);
        
        // is currently selected
        if (idx > -1) {
            $scope.servicesToTest.splice(idx, 1);
        } else {
            // is newly selected
            $scope.servicesToTest.push(serviceName);
        }
    }
    ;
    
    $scope.checkAll = function() {
        if ($scope.selectedAll) {
            $scope.selectedAll = true;
        } else {
            $scope.selectedAll = false;
        }
        angular.forEach($scope.servicesToTest, function(item) {
            item.Selected = $scope.selectedAll;
        });
    
    }
    ;
    
    msg.subscribe('publishProgress');
    msg.subscribe(this);
}
])
