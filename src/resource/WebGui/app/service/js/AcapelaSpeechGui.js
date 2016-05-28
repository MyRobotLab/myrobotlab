angular.module('mrlapp.service.AcapelaSpeechGui', [])
.controller('AcapelaSpeechGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('AcapelaSpeechGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    $scope.text = '';
    $scope.speakingState = '';
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        $scope.voice = service.voice;
    }
    ;
    _self.updateState($scope.service);
    
    // init scope variables
    $scope.text = '';
    
    this.onMsg = function(inMsg) {
        var data = inMsg.data[0];
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data);
            $scope.$apply();
            break;
        case 'onStartSpeaking':
            $scope.speakingState = 'speaking';
            $scope.$apply();
            break;
        case 'onEndSpeaking':
            $scope.speakingState = 'finished speaking';
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
            break;
        }
    }
    ;
    
    //mrl.subscribe($scope.service.name, 'pulse');
    msg.subscribe('publishStartSpeaking');
    msg.subscribe('publishEndSpeaking');
    msg.subscribe(this);
}
]);
