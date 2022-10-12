angular.module('mrlapp.service.LocalSpeechGui', []).controller('LocalSpeechGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('LocalSpeechGuiCtrl')
    var _self = this
    var msg = this.msg

    this.updateState = function(service) {
		$scope.service = service
		$scope.$apply()
	}

    this.onMsg = function(inMsg) {
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(inMsg.data[0])
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.setType = function(type){
        msg.send("set"+type)
    }

	
    $scope.speak = function(text){
        msg.send("speak", text)
    }

    $scope.setVoice  = function(){
        console.log($scope.service.voice.name)
        msg.send("setVoice", $scope.service.voice.name)
    }

    msg.subscribe(this)
}
])
