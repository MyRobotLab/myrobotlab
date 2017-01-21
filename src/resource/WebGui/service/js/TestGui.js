angular.module('mrlapp.service.TestGui', [])
.controller('TestGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', '$sce', function($scope, $log, mrl, $timeout, $sce) {
    $log.info('TestGuiCtrl');
    var _self = this;
    var msg = this.msg;

    $scope.selectedAll = true;
    
    // init scope variables
    $scope.matrix = {};
    $scope.currentProgress = {};
    $scope.currentProgress.percentDone = 0;
    $scope.currentProgress.currentActivity = "ready";
    
    // two service arrays - one is model of "all"
    // the other is current model to test
    $scope.services = [];
    $scope.testPlan = {
        servicesToTest : []
    };
    
    // FIXME - do the same thing for Services - default state is selected
    // FIXME - get this from the service
    $scope.tests = ['PythonScriptTest', 'JunitService', 'PythonScriptExists', 'ServicePageExists'];
    $scope.testsToRun =  angular.copy($scope.tests);
    $scope.trustAsHtml = $sce.trustAsHtml;
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        $scope.matrix = service.matrix;
        $scope.services = service.matrix.services;
        $scope.currentProgress = service.matrix.currentProgress;
        $scope.testPlan.servicesToTest = service.matrix.servicesToTest;
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
    
    $scope.checkAll = function() {
        if ($scope.selectedAll) {
            $scope.selectedAll = true;
            $scope.testPlan.servicesToTest = angular.copy($scope.services);
            $scope.$apply();
        } else {
            $scope.selectedAll = false;
            $scope.testPlan.servicesToTest = [];
            $scope.$apply();
        }
    }
  
    msg.subscribe('publishProgress');
    msg.subscribe(this);
}
])
