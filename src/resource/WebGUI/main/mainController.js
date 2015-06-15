angular.module('mrlapp.main.mainContoller', ['mrlapp.mrl'])

.controller('mainContoller', ['$scope', '$document', 'mrl', 
    function($scope, $document, mrl) {
        
        $scope.onLocalServices = function(msg) {
            $scope.$apply(function() {
                console.log("YAY ! - ");
                console.log(msg);
                
                $scope.name = msg.name;
                $scope.platform = msg.data[0].platform;
                $scope.serviceEnvironment = msg.data[0];
                $scope.serviceDirectory = msg.data[0].serviceDirectory;
                //$scope.platform = msg.data[0].platform;
                console.log("YAY ! - ");

                mrl.subscribe('runtime', 'onRegister');
            //console.log('trying to launch ' + service.name + ' of ' + service.simpleName + ' / ' + service.serviceClass);
            });
        };

        //mrl.subscribeToMethod($scope.onLocalServices);
        // subscribe to a particular method
        // probably should be refactored to allow Hello, I'm {{name}} - platform  request
        // which should come back (server response -> Hello I'm Runtime "runtime" - here is my service Environment

        // subscribe to onLocalServices
        mrl.subscribeToMethod($scope.onLocalServices, 'onLocalServices');
        // connect and us message
        mrl.connect(document.location.origin.toString() + '/api/messages');
    }]);
