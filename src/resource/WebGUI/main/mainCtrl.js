angular.module('mrlapp.main.mainCtrl', ['mrlapp.mrl'])
.controller('mainCtrl', ['$scope', '$document', 'mrl', 'ServiceSvc', 
    function($scope, $document, mrl, ServiceSvc) {
        
        console.log('is connected: ' + mrl.isConnected());
        
        //service-panels & update-routine
        $scope.services = ServiceSvc.getServices();
        var panelsUpdated = function() {
            $scope.services = ServiceSvc.getServices();
            $scope.$apply();
        };
        ServiceSvc.subscribeToUpdates(panelsUpdated);
        
        $scope.gateway = mrl.getGateway();
        $scope.runtime = mrl.getRuntime();
        $scope.platform = mrl.getPlatform();
        $scope.registry = mrl.getRegistry();
        
        $scope.guiData = {};
        
        for (var name in $scope.registry) {
            if ($scope.registry.hasOwnProperty(name)) {
                var service = $scope.registry[name];
                var data = {};
                data.service = service;
                $scope.guiData[name] = data;
                //_self.addNewServiceGUI(service);
            }
        }

    //$scope.serviceEnvironment = msg.data[0];
    //$scope.serviceDirectory = msg.data[0].serviceDirectory;
    //$scope.platform = msg.data[0].platform;
    //mrl.subscribe('runtime', 'registered');
    
    }]);
