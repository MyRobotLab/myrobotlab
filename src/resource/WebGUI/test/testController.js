angular.module('mrlapp.test.testController', ['mrlapp.mrl'])

.controller('testController', ['$scope', '$document', 'mrl', 
    function($scope, $document, mrl) {
        
        $scope.onLocalServices = function(msg) {
            $scope.$apply(function() {
                console.log("YAY ! - ");
                console.log(msg);

                var runtimeName = mrl.runtime.name;
                var gatewayName = mrl.gateway.name;
                
                $scope.name = msg.name;
                $scope.gateway = mrl.gateway;
                $scope.runtime = mrl.runtime;
                $scope.platform = mrl.platform;
                $scope.serviceEnvironment = msg.data[0];
                $scope.serviceDirectory = msg.data[0].serviceDirectory;
                //$scope.platform = msg.data[0].platform;
                console.log("YAY ! - ");
                
                mrl.sendTo(runtimeName, "start", "clock01", "Clock");
                
                mrl.subscribe(runtimeName, 'registered');
            //console.log('trying to launch ' + service.name + ' of ' + service.simpleName + ' / ' + service.serviceClass);
            });
        };

        mrl.connect(document.location.origin.toString() + '/api/messages');
        // FIXME - right now this MUST come after the connect so that the
        // callback to mrl to processes the data of the connection
        mrl.subscribeToMethod($scope.onLocalServices, 'onLocalServices');
    
    }]);
