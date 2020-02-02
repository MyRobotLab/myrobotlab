angular.module('mrlapp.service.WebkitSpeechSynthesisGui', []).controller('WebkitSpeechSynthesisGuiCtrl', ['$scope', '$log', 'mrl', '$uibModal', function($scope, $log, mrl, $uibModal) {
    $log.info('WebkitSpeechSynthesisGuiCtrl')
    var _self = this
    var msg = this.msg

    
    $scope.speak = function() {
        if ('speechSynthesis'in window) {
            // Synthesis support. Make your web apps talk!
            var synth = window.speechSynthesis;

            voices = synth.getVoices();
            for (var i = 0; i < voices.length; i++) {
                console.log("Voice " + i.toString() + ' ' + voices[i].name + ' ' + voices[i].uri);
            }

            var utterThis = new SpeechSynthesisUtterance("Hello this is a test, can you hear me ... why have i been so quite for so long");
            utterThis.voice = voices[2];
            utterThis.pitch = 1.0
            utterThis.rate = 1.0
            synth.speak(utterThis)

        }
    }

    this.updateState = function(service) {
		$scope.service = service
	}

    // console.log('mary', $scope.service)

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
    

    // I suspect speak is not "setup" like other functions and is not accessable like others in the
    // theml e.g. msg.speak - so got to figure that out or temporarily create a $scope.speak kludge
    $scope.speak = function(text){
        msg.send("speak", text)
        //console.log($scope.service.voice.name)
    }

    $scope.setVoice  = function(text){
        console.log($scope.service.voice.name)
        msg.send("setVoice", text.name)
        // msg.send("broadcastState")
    }

    msg.subscribe(this)
}
])