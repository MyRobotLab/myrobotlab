angular.module('mrlapp.service.WebkitSpeechSynthesisGui', []).controller('WebkitSpeechSynthesisGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('WebkitSpeechSynthesisGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.voiceToIndexMap = {}
    $scope.voiceIndex = 0
    $scope.voices = []
    $scope.voiceSelected = null
    $scope.text = ""

    this.updateState = function(service) {
        $scope.service = service
        $scope.voiceIndex = service.voiceIndex
        if ($scope.voices.length == 0) {
            for (let i = 0; i < service.voiceList.length; ++i) {
                $scope.voices.push(service.voiceList[i])
            }
        }
        $scope.voiceSelected = service.voice.name
        $scope.$apply()
    }

    // Chrome loads voices asynchronously.
    window.speechSynthesis.onvoiceschanged = function(e) {
        populateVoiceList()
    }

    window.speechSynthesis.onvoiceschanged = function(e) {
        populateVoiceList()
    }

    let webkitSpeak = function(text) {
        $scope.text = text
        $scope.$apply()
        let synth = window.speechSynthesis;
        let utterThis = new SpeechSynthesisUtterance(text);
        utterThis.voice = $scope.voices[$scope.voiceIndex];
        utterThis.pitch = 1.0
        utterThis.rate = 1.0

        // hook start end events
        utterThis.addEventListener('start', (event)=>{
            console.log(`Utterance has started being spoken after ${event.elapsedTime} seconds.`)
            // publishing AudioData object
            // msg.send('publishAudioStart', {"filename":text, "volume":1.0})
            msg.send('publishStartSpeaking', text)
        }
        )

        utterThis.addEventListener('end', (event)=>{
            console.log(`Utterance has finished being spoken after ${event.elapsedTime} seconds.`)
            msg.send('publishEndSpeaking', text)
        }
        )

        synth.speak(utterThis)
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onVoiceIndex':
            $scope.voiceIndex = data
            break

        case 'onWebkitSpeak':
            webkitSpeak(data)
            break

        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    let populateVoiceList = function() {
        if (typeof speechSynthesis === 'undefined') {
            console.info('no voices defined')
            return;
        }

        $scope.voices = speechSynthesis.getVoices();

        for (let v = 0; v < $scope.voices.length; v++) {
            let voice = $scope.voices[v];
            console.info('found voice ' + v + ' ' + voice.name)
            msg.send("addWebKitVoice", v, voice.name, voice.lang, voice.default)
        }

        msg.send('broadcastState')
    }

    // sadly this does not work immediately - the voices will not get populated until the user clicks a button
    // speechSynthesis.speak() without user activation is no longer allowed since M71, around December 2018. 
    // See https://www.chromestatus.com/feature/5687444770914304 for more details speechSynthesisMessage    
    populateVoiceList();

    $scope.setVoice = function(text) {
        msg.send('setVoice', text)
        msg.send('broadcastState')
    }

    $scope.setMute = function(mute) {
        msg.send('setMute', mute)
        populateVoiceList()
    }

    msg.subscribe('webkitSpeak')
    msg.subscribe('publishVoiceIndex')
    msg.send('setMute', true)
    msg.subscribe(this)
}
])
