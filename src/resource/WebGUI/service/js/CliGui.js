angular.module('mrlapp.service.CliGui', [])
.controller('CliGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('CliGuiCtrl');
    
    // get fresh copy
    var service = mrl.getService($scope.service.name);
    var name = $scope.service.name;
    
    $scope.panel.onMsg = function(msg) {
        switch (msg.method) {
        case 'onStdout':
            $scope.cli = $scope.cli + '\n' + msg.data[0];
            $scope.$apply();
            break;
        case 'onClockStarted':
            $scope.label = "Stop";
            $scope.$apply();
            break;
        case 'onClockStopped':
            $scope.label = "Start";
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + msg.method);
            break;
        }
    }
    ;
    
    //$scope.myStyle = ".boxsizingBorder {-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;}";    
    // $scope.myStyle = "{'background-color':'black';}";
    
    var buffer = "";
    
    $scope.keyPress = function(event) {
        var keyCode = event.keyCode;
        var c = String.fromCharCode((96 <= keyCode && keyCode <= 105)? keyCode-48 : keyCode);
        $log.info('keyPress ', keyCode);
        if (keyCode == 13) {
            mrl.sendTo(name, 'process', buffer);
            buffer = '';
            return;
        }
        buffer = buffer + String.fromCharCode(keyCode);
    }
    ;
    
    mrl.subscribe($scope.service.name, 'stdout');
    
    //after you're done with setting up your service-panel, call this method
    $scope.panel.initDone();
}
]);
