angular.module('mrlapp.service.runtimegui', [])

.controller('RuntimeGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
        $log.info('RuntimeGuiCtrl');
        
        var getSimpleName = function(fullname) {
            return (fullname.substring(fullname.lastIndexOf(".") + 1));
        }
        
        $scope.service = mrl.getService($scope.service.name);
        var platform = $scope.service.platform;
        // make the platform string
        $scope.platform = platform.arch + "." + platform.bitness + "." + platform.os;
        $scope.version = platform.mrlVersion;
        
        $scope.panel.onMsg = function(msg) {
            
            switch (msg.method) {
                case 'onPulse':
                    $scope.pulseData = msg.data[0];
                    $scope.$apply();
                    break;
                case 'onClockStarted':
                    $scope.label = "Stop";
                    $scope.intervalDisabled = true;
                    $scope.$apply();
                    break;
                case 'onClockStopped':
                    $scope.label = "Start";
                    $scope.intervalDisabled = false;
                    $scope.$apply();
                    break;
                default:
                    $log.error("ERROR - unhandled method " + $scope.name + " " + msg.method);
                    break;
            }
        };
        
        $scope.localServiceData = $scope.service.repo.localServiceData.serviceTypes;
        //mrl.subscribe($scope.service.name, 'pulse');            
        
        $scope.newType = undefined;
        $scope.possibleServices = [];

        // pump model data from repo
        for (var property in $scope.service.repo.localServiceData.serviceTypes) {
            if ($scope.service.repo.localServiceData.serviceTypes.hasOwnProperty(property)) {
                var serviceType = $scope.service.repo.localServiceData.serviceTypes[property];
                var model = {};
                model.name = getSimpleName(property);
                model.img = model.name + '.png';
                model.alt = serviceType.description;
                $scope.possibleServices.push(model);
            }
        }
        
        $scope.possibleServices = $scope.possibleServices;
        
        $scope.start = function(newName, newTypeModel) {
            mrl.sendTo($scope.service.name, "start", newName, newTypeModel.name);
        }
    
    }]);
