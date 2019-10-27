angular.module('mrlapp.service.AudioFileGui', [])
.controller('AudioFileGuiCtrl', ['$log', '$scope', 'mrl', function($log, $scope, mrl) {
    $log.info('AudioFileGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    // init
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        $scope.stopCapture      = service.stopCapture;
        $scope.soundCaptured    = service.soundCaptured;
        $scope.captureAudio     = service.captureAudio;
        $scope.stopAudioFile = service.stopAudioFile;
        $scope.playAudio        = service.playAudio;
    }
    ;
    
    _self.updateState($scope.service);
    
    this.onMsg = function(inMsg) {
        var data = inMsg.data[0];
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data);
            $scope.$apply();
            break;
        default:
            $log.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method);
            break;
        }
        ;
    
    }
    ;
    
    msg.subscribe(this);
}
]);
