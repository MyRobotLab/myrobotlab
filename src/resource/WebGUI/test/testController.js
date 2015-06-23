// FIXME - methods can be simplified if contexts are known,
// if Type is known
// if name is known

// FIXME - notifylists should not contain DUPLICATES !!!!

angular.module('mrlapp.test.testController', ['mrlapp.mrl'])
.controller('testController', ['$scope', '$document', 'mrl', 
    function($scope, $document, mrl) {
        _self = this;
        
        console.log('is connected: ' + mrl.isConnected());
        
        $scope.testName = "clock01";
        $scope.testType = "Clock";
        
        $scope.gateway = mrl.getGateway();
        $scope.runtime = mrl.getRuntime();
        $scope.platform = mrl.getPlatform();
        $scope.registry = mrl.getRegistry();
        
        $scope.guiData = {};

        // TODO - if !mrl.connected() mrl.connect ! .. onConnect function


        // ===========Clock Type Begin===========
        $scope.onClockMsg = function(msg) {
            $scope.$apply(function() {
                console.log("Clock Msg ! - ");
                if (msg.method == "onPulse") {
                    var pulseData = msg.data[0];
                    //$scope.serviceDirectory[msg.sender].pulseData = pulseData;
                    $scope.guiData[msg.sender].pulseData = pulseData;
                }
            
            });
        }
        
        this.loadClockGUI = function(service) {

            // load data bindings for this type
            $scope.guiData[service.name]['pulseData'] = '';

            // create message bindings
            // FIXME - route all 'named' msgs to a ServiceGUI
            mrl.subscribeToService($scope.onClockMsg, service.name);
            
            mrl.subscribe(service.name, 'pulse');
        
        }
        // ===========Clock Type End===========


        // ===========Runtime Type Begin===========
        $scope.onRuntimeMsg = function(msg) {
            $scope.$apply(function() {
                console.log("Runtime Msg ! - ");
                if (msg.method == "onRegistered") {
                    var newService = msg.data[0];
                    //$scope.serviceDirectory[msg.sender].pulseData = pulseData;
                    // $scope.guiData[msg.sender].pulseData = pulseData;
                    _self.addNewServiceGUI(newService);
                }
                if (msg.method == "onReleased") {
                    _self.removeServiceGUI(msg.data[0]);
                }
            
            });
        }
        
        this.loadRuntimeGUI = function(service) {
            // 2 Links need to be set 
            // 1. is from JavaScript msg handler to a callback function
            // this subscribes to (all) the JavaScript callback for that service
            mrl.subscribeToService($scope.onRuntimeMsg, service.name);
            // 2. now anything sent to the webgui will get relayed to us
            // send a subscription from the "real" clock to the WebGUI
            //mrl.sendTo(mrl.getGateway().name, "subscribe", service.name, "pulse");
            
            mrl.subscribe(service.name, "registered");
            mrl.subscribe(service.name, "released");
        
        }
        // ===========Runtime Type End===========
        
        
        this.addNewServiceGUI = function(service) {
            // gui data object ...
            // I "hope" name is a reference...
            $scope.guiData[service.name] = {};
            $scope.guiData[service.name]['name'] = service.name;
            $scope.guiData[service.name]['service'] = service;
            
            var type = service.simpleName;
            try {
                
                _self['load' + type + 'GUI'](service);
            
            } catch (e) {
                console.log('Error: could not load type [load' + type + 'GUI] probably does not exist', e);
            }
        
        }
        
        this.removeServiceGUI = function(name) {
            delete $scope.guiData[service.name];
        }


        // Pattern - in context of workspace - scan through all types - 
        // initially - scan through registry and subscribe based on service type
        for (var name in $scope.registry) {
            if ($scope.registry.hasOwnProperty(name)) {
                var service = $scope.registry[name];
                _self.addNewServiceGUI(service);
            }
        }

        // FIXME - the common pattern - based on type
        // send out the subcriptions you are interested in

        // FIXME - must be done in mrl
        //mrl.subscribeToService($scope.onRuntimeMsg, runtimeName);

        // mrl.subscribe(runtimeName, 'registered');
        //console.log('trying to launch ' + service.name + ' of ' + service.simpleName + ' / ' + service.serviceClass);
        // });
        //};
        
        $scope.start = function() {
            mrl.sendTo(mrl.getRuntime().name, "start", $scope.testName, $scope.testType);
        }

    //setState();
    
    }]);
