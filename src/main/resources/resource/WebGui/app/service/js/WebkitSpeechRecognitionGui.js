angular.module('mrlapp.service.WebkitSpeechRecognitionGui', []).controller('WebkitSpeechRecognitionGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    console.log('WebkitSpeechRecognitionGuiCtrl')

    var _self = this
    var msg = this.msg
    let recognizer = null

    $scope.restartCnt = 0
    $scope.interimTranscript = ''
    $scope.publishedText = ''
    $scope.wakeWord = null

    // corresponds to internal RecognizedResult class
    // in AbstractSpeechREcognizer
    $scope.recognizedResult = {
        text: null,
        confidence: null,
        isFinal: false
    }

    $scope.selectedLanguage = "en-US"
    $scope.startTimestamp = null
    $scope.stopRequested = false
    $scope.error = null
    // original error code
    $scope.log = []
    $scope.webkitSupport = true
    $scope.micImage = '../WebkitSpeechRecognition/mic.png'

    // this is really a js service
    // and this is the initial service state we want
    $scope.service = {
        config: {isRecording: false},
        status: null
    }

    // control through the ui ?
    $scope.changeListeningState = function() {
        if (!$scope.isRecording) {
            $scope.setState('start')
            mrl.sendTo($scope.name,'startListening') 
        } else {
            $scope.setState('stop')
            mrl.sendTo($scope.name,'stopListening') 
        }
    }

    $scope.setState = function(statusKey, event) {
        console.debug('status ' + statusKey)

        $scope.service.status = statusKey

        switch (statusKey) {
        case 'onstart':
            $scope.isRecording = true
            $scope.micImage = '../WebkitSpeechRecognition/mic-animate.gif'
            $scope.startTimestamp = new Date().getTime()
            console.debug('speak now')
            $scope.error = null
            $scope.$apply()
            break

        case 'onresult':
            $scope.interimTranscript = ''

            console.debug('onresult has ' + event.results.length + ' results')
            for (var i = event.resultIndex; i < event.results.length; ++i) {
                let data = event.results[i][0]

                $scope.recognizedResult = {
                    text: data.transcript,
                    confidence: (Math.round(data.confidence * 100) / 100).toFixed(2),
                    isFinal: false
                }

                if (event.results[i].isFinal) {
                    $scope.recognizedResult.isFinal = true
                    msg.send('processResults', [$scope.recognizedResult])
                    // $scope.log.unshift($scope.recognizedResult)
                    $scope.$apply()
                } else {
                    // weird handling of this ... FIXME - just pubish it all and set the correct final ..
                    $scope.interimTranscript += data.transcript
                }
            }
            break

        case 'onend':
            $scope.isRecording = false
            $scope.micImage = '../WebkitSpeechRecognition/mic-slash.png'
            if (!$scope.stopRequested) {
                $scope.restartCnt += 1
                recognizer.start()
                console.log('onend - but stop was not requested')
            }
            $scope.$apply()
            break

        case 'onerror':

            $scope.error = event.error

            /*
            let errorTs = new Date().getTime()
            if ((errorTs - $scope.startTimestamp) < 100) {
                $scope.errorText += ' - high error rate - is another tab listening?'          

            }
            */

            if (event.error != 'no-speech') {
                console.error('onerror - stopping- ' + $scope.error)

                // requesting recording STOP - this will finalize stop
                msg.send('stopRecording')
                $scope.setState('stop')
            } else {
                console.debug('no-speech pending restart')
            }

            $scope.$apply()

            break

        case 'stop':
            $scope.stopRequested = true
            recognizer.stop()
            msg.send('stopListening')
            break

        case 'start':

            $scope.stopRequested = false
            if ($scope.isRecording) {
                recognizer.stop()
            }
            recognizer.lang = $scope.selectedLanguage
            recognizer.start()
            break
        default:
            console.error("unhandled status " + statusKey)
            break
        }

    }

    // ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window) 
    // if (!('webkitSpeechRecognition'in window)) {
    if (!('SpeechRecognition'in window || 'webkitSpeechRecognition'in window)) {
        $scope.webkitSupport = false
    } else {
        // chrome is being used
        console.info('creating new recognizer')
        recognizer = new webkitSpeechRecognition()
        recognizer.continuous = false
        recognizer.interimResults = true

        recognizer.onstart = function() {
            $scope.setState('onstart')
        }

        recognizer.onerror = function(event) {
            $scope.setState('onerror', event)
        }

        recognizer.onend = function() {
            $scope.setState('onend')
        }

        recognizer.onresult = function(event) {
            $scope.setState('onresult', event)
        }
    }

    $scope.setLanguage = function() {
        recognizer.lang = $scope.selectedLanguage
        // sync'ing runtime platform locale
        msg.send("setLocale", $scope.selectedLanguage)
        if ($scope.isRecording) {
            recognizer.stop()
        }
    }

    $scope.setLanguageFromService = function(lang) {
        // recognizer.lang = $scope.selectedLanguage
        recognizer.lang = lang
        // sync'ing runtime platform locale
        // msg.send("setLocale", $scope.selectedLanguage)
        if ($scope.isRecording) {
            recognizer.stop()
        }
    }

    this.updateState = function(service) {
        // $scope.service is old data
        // service is new data

        if ($scope.isRecording && !service.config.recording) {
            $scope.setState('stop')
        }
        if (!$scope.isRecording && service.config.recording) {
            $scope.setState('start')
        }

        /*
        Object.keys(service.locales).forEach(function(key) {
           if (service.locales[key].language == service.locale.language){
               $scope.selectedLanguage = service.locales[key].tag;     
            }
        })
        */
        let tag = service.locale.tag.substring(0,2)
        if (tag == 'fr'){
            tag = 'fr-FR'
        } else if (tag == 'de'){
            tag = 'de-DE'
        } else if (tag == 'en'){
            tag = 'en-US'
        } else if (tag == 'es'){
            tag = 'es-ES'
        } else if (tag == 'fi'){
            tag = 'fi-FI'
        } else if (tag == 'fr'){
            tag = 'fr-FR'
        } else if (tag == 'hi'){
            tag = 'hi-IN'
        } else if (tag == 'it'){
            tag = 'it-IT'
        } else if (tag == 'nl'){
            tag = 'nl-NL'
        } else if (tag == 'pt'){
            tag = 'pt-PT'
        } else if (tag == 'ru'){
            tag = 'ru-RU'
        } else if (tag == 'tr'){
            tag = 'tr-TR'
        }

        if (tag != $scope.selectedLanguage){
            $scope.selectedLanguage = tag
            $scope.setLanguageFromService(tag)            
        }

        // update en-mass
        $scope.service = service

        if (service.config.wakeWord){
            service.wakeWord = service.config.wakeWord
        }

    }

    this.onMsg = function(msg) {
        console.log("webkit msg " + msg.method)
        let data = msg.data[0]
        switch (msg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onListeningEvent':
            // $scope.log.unshift($scope.recognizedResult)
            if (data.isSpeaking && data.confidence) {
                data.text = "heard while speaking : " + data.text
            } else if (data.isSpeaking) {
                data.text = "speaking : " + data.text
            }
            $scope.log.unshift(data)
            $scope.$apply()
            break
        case 'onOnStartSpeaking':
            console.log("Started speaking, stop listening.")
            $scope.log.unshift({
                ts: ts = new Date().getTime(),
                text: "speaking : " + data
            })
            //$scope.startRecognition()
            break
        case 'onOnEndSpeaking':
            console.log("Stopped speaking, start listening.")

            /*
            if (!$scope.isRecording) {
                $scope.startRecognition()
            }*/
            break
        default:
            console.log("Unknown Message recieved." + msg.method)
            break
        }
    }

    // $scope.setState('start')

    msg.subscribe('publishListeningEvent')
    // msg.subscribe('setLocale')
    // msg.subscribe('onStartSpeaking')

    /*
    msg.subscribe('onStartSpeaking')
    msg.subscribe('onEndSpeaking')
    msg.subscribe('onStartListening')
    msg.subscribe('onStopListening')
    */
    // msg.send('processResults', [{ text:"worky !!!!", confidence:0.9999 }])

    // $scope.setState('start')
    msg.subscribe(this)
    msg.send('broadcastState')


}
])
