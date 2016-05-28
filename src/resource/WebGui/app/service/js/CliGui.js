angular.module('mrlapp.service.CliGui', [])
.controller('CliGuiCtrl', ['$scope', '$log', function($scope, $log) {
    $log.info('CliGuiCtrl');
    var _self = this;
    var msg = this.msg;
    var buffer = "";
    $scope.cli = "";
    
    this.updateState = function(service) {
        $scope.service = service;
    }
    ;
    _self.updateState($scope.service);
    
    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onStdout':
            $scope.cli = $scope.cli + '\n' + inMsg.data[0];
            $scope.$apply();
            break;
        case 'onPrompt':
            $scope.cli = $scope.cli + '\n' + inMsg.data[0];
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + inMsg.method);
            break;
        }
    }
    ;
    
    $scope.keyPress = function(event) {
        var keyCode = event.keyCode;
        var c = String.fromCharCode((96 <= keyCode && keyCode <= 105) ? keyCode - 48 : keyCode);
        $log.info('keyPress ', keyCode);
        if (keyCode == 13) {
            msg.send('process', buffer);
            buffer = '';
            return;
        }
        buffer = buffer + String.fromCharCode(keyCode);
    }
    ;
    
    msg.subscribe('stdout');    
    msg.subscribe('getPrompt');  
    msg.send('getPrompt');  
    msg.subscribe(this);
}
]);
