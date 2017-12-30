angular.module('mrlapp.service.TestGui', [])
.controller('TestGuiCtrl', ['$scope', '$log', 'mrl', '$timeout', '$sce', function($scope, $log, mrl, $timeout, $sce) {
    $log.info('TestGuiCtrl');
    var _self = this;
    var msg = this.msg;
    var services = [];

    $scope.selectedAll = true;
    
    // init scope variables
    $scope.matrix = {};
    $scope.currentProgress = {};
    $scope.currentProgress.percentDone = 0;
    $scope.currentProgress.currentActivity = "ready";
    $scope.currentTest = {};
    $scope.lastTest = {};
    
    // two service arrays - one is model of "all"
    // the other is current model to test
    $scope.services = [];
    
    // FIXME - do the same thing for Services - default state is selected
    // FIXME - get this from the service
    $scope.tests = [];

    // "if your not using a dot - your doing it wrong" - Sooooooo true
    // fought with $scope.servicesToTest = x for such a long time !!!!
    $scope.testPlan = {
        servicesToTest : []
    }
    
    // $scope.testsToRun =  angular.copy($scope.tests);
    // $scope.servicesToTest = [];// angular.copy($scope.tests);

    $scope.trustAsHtml = $sce.trustAsHtml;
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {

        _self.services = service.services;

        // globals
        $scope.service  = service;
        $scope.matrix   = service.matrix;
        var matrix      = service.matrix;

        $scope.errors   = service.errors;

        // all possible tests
        $scope.tests = service.tests;
        // all possible services
        $scope.services = service.services;
        
        $scope.currentProgress = matrix.currentProgress;

        // test plan
        $scope.testPlan.servicesToTest = [];// matrix.servicesToTest;

        for (var i = 0; i < service.services.length; ++i){
            var serviceType = service.services[i];
            if (serviceType.available){
                $scope.testPlan.servicesToTest.push(serviceType.simpleName);
            }
        }

        $scope.testsToRun     = matrix.testsToRun;
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
            // $scope.servicesToTest = angular.copy($scope.services);
            // $scope.services = _self.services;
            $scope.testPlan.servicesToTest = $scope.services.map(function(item) { return item.simpleName; });
            // $scope.
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
