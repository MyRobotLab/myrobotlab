angular.module('mrlapp.service.WebkitSpeechSynthesisGui', []).controller('WebkitSpeechSynthesisGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('WebkitSpeechSynthesisGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.voiceToIndexMap = {}



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
         case 'onVoiceIndex':
            break
            
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }
    

    // I suspect speak is not "setup" like other functions and is not accessable like others in the
    // theml e.g. msg.speak - so got to figure that out or temporarily create a $scope.speak kludge
    $scope.speak = function(text){
         if ('speechSynthesis'in window) {
            // Synthesis support. Make your web apps talk!
            var synth = window.speechSynthesis;

            var utterThis = new SpeechSynthesisUtterance(text);
            utterThis.voice = voices[2];
            utterThis.pitch = 1.0
            utterThis.rate = 1.0
            synth.speak(utterThis)

        }
    }


    $scope.setVoice  = function(text){        
        msg.send("setVoice", text.name)
    }

     if ('speechSynthesis'in window) {
            // Synthesis support. Make your web apps talk!
            var synth = window.speechSynthesis;

            voices = synth.getVoices();
            
            for (var i = 0; i < voices.length; i++) {
                console.log("Voice " + i.toString() + ' ' + voices[i].name + ' ' + voices[i].uri);
                msg.send("addWebKitVoice", i, voices[i].name, voices[i].lang, voices[i].default)
            }
     }

    msg.subscribe('publishVoiceIndex')
    msg.subscribe(this)
}
])