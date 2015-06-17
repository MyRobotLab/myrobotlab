angular.module('mrlapp.test.testController', ['mrlapp.mrl'])

.controller('testController', ['$scope', '$document', 'mrl', 
    function($scope, $document, mrl) {

        // TODO - if !mrl.connected() mrl.connect ! .. onConnect function
        $scope.testName = "blah01";
        
        $scope.onRuntimeMsg = function(msg) {
             $scope.$apply(function() {
                console.log("SUPER YAY ! - ");
                if (msg.method == "onRegistered"){
                    var newService = msg.data[0];
                    $scope.serviceDirectory[newService.name] = newService;
                }
                
             });
        }
        
        $scope.onLocalServices = function(msg) {
            // This is response on the very first
            // URL GET request http://....
            // the response will contain our gateway & runtime info
            // from that we can subscribe to all Runtime messages sent
            // to the WebGUI
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

                mrl.subscribeToService($scope.onRuntimeMsg, runtimeName);

            // mrl.subscribe(runtimeName, 'registered');
            //console.log('trying to launch ' + service.name + ' of ' + service.simpleName + ' / ' + service.serviceClass);
            });
        };
        
        $scope.start = function() {
            mrl.sendTo(mrl.runtime.name, "start", $scope.testName, $scope.testType);
        }

        
        mrl.connect(document.location.origin.toString() + '/api/messages');
        // FIXME - right now this MUST come after the connect so that the
        // callback to mrl to processes the data of the connection
        mrl.subscribeToMethod($scope.onLocalServices, 'onLocalServices');
    
    }]);
