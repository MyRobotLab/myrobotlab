angular.module('mrlapp.service.runtimegui', [])

.controller('RuntimeGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
        console.log('RuntimeGuiCtrl');
        $scope.service = mrl.getService($scope.service.name);
        var platform = $scope.service.platform;
        // make the platform string
        $scope.platform = platform.arch + "." + platform.bitness + "." + platform.os;
    
    }]);
