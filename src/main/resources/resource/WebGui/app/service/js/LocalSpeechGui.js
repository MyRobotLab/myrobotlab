angular.module('mrlapp.service.LocalSpeechGui', []).controller('LocalSpeechGuiCtrl', ['$scope', '$log', 'mrl', '$uibModal', function($scope, $log, mrl, $uibModal) {
    $log.info(' LocalSpeechGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.voiceSelected = null

    this.updateState = function(service) {
		$scope.service = service
		if (service.voice != null){
			$scope.voiceSelected = service.voice
		}		
	}

    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0])
            $scope.$apply()
            break
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }
    
    
    $scope.speak = function(text){
        console.log($scope.service.voice.name)
        msg.send("speak", text)
    }

    $scope.setVoice  = function(text){
        console.log($scope.service.voice.name)
        msg.send("setVoice", text.name)
        // msg.send("broadcastState")
    }

    msg.subscribe(this)
}
])
