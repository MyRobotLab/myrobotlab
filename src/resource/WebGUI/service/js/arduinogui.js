angular.module('mrlapp.service.arduinogui', [])

.controller('ArduinoGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
        console.log('ArduinoGuiCtrl');
        $scope.service = mrl.getService($scope.service.name);
        $scope.showMRLComm = true;
        
        $scope.toggleShowMRLComm = function() {
            $scope.showMRLComm = ($scope.showMRLComm) ? false : true;
        }
        
        $scope.aceLoaded = function(_editor) {
            // Options
            _editor.setReadOnly(true);
            _editor.$blockScrolling = Infinity;
            _editor.setValue($scope.service.sketch.data);
        
        };
        
        $scope.aceChanged = function(e) {
        //
        };
        
        $scope.tabs = [
            {title: 'Dynamic Title 1',content: 'Dynamic content 1'}, 
            {title: 'Dynamic Title 2',content: 'Dynamic content 2',disabled: true}
        ];
    
    }]);
