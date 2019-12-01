angular.module('mrlapp.service.AudioCaptureGui', [])
.controller('AudioCaptureGuiCtrl', ['$log', '$scope', 'mrl', function($log, $scope, mrl) {
    $log.info('AudioCaptureGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    // init
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        $scope.stopCapture      = service.stopCapture;
        $scope.soundCaptured    = service.soundCaptured;
        $scope.captureAudio     = service.captureAudio;
        $scope.stopAudioCapture = service.stopAudioCapture;
        $scope.playAudio        = service.playAudio;
    }
    ;
  
    
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
