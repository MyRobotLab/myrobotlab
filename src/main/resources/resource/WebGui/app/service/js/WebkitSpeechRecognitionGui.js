angular.module('mrlapp.service.WebkitSpeechRecognitionGui', []).controller('WebkitSpeechRecognitionGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    console.log('WebkitSpeechRecognitionGuiCtrl')

    var _self = this
    var msg = this.msg
    let recognizer = null

    $scope.restartCnt = 0
    $scope.interimTranscript = ''
    $scope.publishedText = ''

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
    $scope.error = null // original error code
    $scope.log = []
    $scope.webkitSupport = true
    $scope.micImage = '../WebkitSpeechRecognition/mic.png'


    // this is really a js service
    // and this is the initial service state we want
    $scope.service = {
        isRecording: false,
        status: null
    }

    // control through the ui ?
    $scope.changeListeningState = function() {
        if (!$scope.isRecording) {
            $scope.setState('start')
            // msg.send('startListening') 
        } else {
            $scope.setState('stop')
            // msg.send('stopListening')
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
    if (!('SpeechRecognition' in window || 'webkitSpeechRecognition' in window)) {
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
        if ($scope.isRecording) {
            recognizer.stop()
        }
    }

    this.updateState = function(service) {
        // $scope.service is old data
        // service is new data

        if ($scope.isRecording && !service.isRecording) {
            $scope.setState('stop')
        }
        if (!$scope.isRecording && service.isRecording) {
            $scope.setState('start')
        }

        // update en-mass
        $scope.service = service
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
            if (data.isSpeaking && data.confidence){
                data.text = "heard while speaking : " + data.text
            } else if (data.isSpeaking){
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

}
])
