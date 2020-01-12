angular.module('mrlapp.service.WebkitSpeechRecognitionGui', []).controller('WebkitSpeechRecognitionGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    console.log('WebkitSpeechRecognitionGuiCtrl')

    var _self = this
    var msg = this.msg

    $scope.languages = {
        "Afrikaans": "af-ZA",
        "Bahasa Indonesia": "id-ID",
        "Bahasa Melayu": "ms-MY",
        "Català": "ca-ES",
        "Dansk": "da-DK",
        "Deutsch": "de-DE",
        "English - Australia": "en-AU",
        "English - Canada": "en-CA",
        "English - India": "en-IN",
        "English - New Zealand": "en-NZ",
        "English - South Africa": "en-ZA",
        "English - United Kingdom": "en-GB",
        "English - United States": "en-US",
        "Español - Argentina": "es-AR",
        "Español - Bolivia": "es-BO",
        "Español - Chile": "es-CL",
        "Español - Colombia": "es-CO",
        "Español - Costa Rica": "es-CR",
        "Español - Ecuador": "es-EC",
        "Español - El Salvador": "es-SV",
        "Español - España": "es-ES",
        "Español - Estados Unidos": "es-US",
        "Español - Guatemala": "es-GT",
        "Español - Honduras": "es-HN",
        "Español - México": "es-MX",
        "Español - Nicaragua": "es-NI",
        "Español - Panamá": "es-PA",
        "Español - Paraguay": "es-PY",
        "Español - Perú": "es-PE",
        "Español - Puerto Rico": "es-PR",
        "Español - República Dominicana": "es-DO",
        "Español - Uruguay": "es-UY",
        "Español - Venezuela": "es-VE",
        "Euskara": "eu-ES",
        "Filipino": "fil-PH",
        "Français": "fr-FR",
        "Galego": "gl-ES",
        "Hindi - हिंदी": "hi-IN",
        "Hrvatski": "hr_HR",
        "IsiZulu": "zu-ZA",
        "Italiano - Italia": "it-IT",
        "Italiano - Svizzera": "it-CH",
        "Lietuvių": "lt-LT",
        "Magyar": "hu-HU",
        "Nederlands": "nl-NL",
        "Norsk bokmål": "nb-NO",
        "Polski": "pl-PL",
        "Português - Brasil": "pt-BR",
        "Português - Portugal": "pt-PT",
        "Pусский": "ru-RU",
        "Română": "ro-RO",
        "Slovenčina": "sk-SK",
        "Slovenščina": "sl-SI",
        "Suomi": "fi-FI",
        "Svenska": "sv-SE",
        "Tiếng Việt": "vi-VN",
        "Türkçe": "tr-TR",
        "Íslenska": "is-IS",
        "Čeština": "cs-CZ",
        "Ελληνικά": "el-GR",
        "Српски": "sr-RS",
        "Українська": "uk-UA",
        "български": "bg-BG",
        "ภาษาไทย": "th-TH",
        "中文 - 中文 (台灣)": "cmn-Hant-TW",
        "中文 - 普通话 (中国大陆)": "cmn-Hans-CN",
        "中文 - 普通话 (香港)": "cmn-Hans-HK",
        "中文 - 粵語 (香港)": "yue-Hant-HK",
        "日本語": "ja-JP",
        "한국어": "ko-KR",
    }

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
    $scope.errorText = null
    $scope.log = []

    // this is really a js service
    // and this is the initial service state we want
    $scope.service = {
        isListening: false,
        status: null,
        img: '../WebkitSpeechRecognition/mic.png',
        webkitSupport: true
    }

    $scope.changeListeningState = function() {
        if (!$scope.service.isListening) {
            $scope.setState('start')
        } else {
            $scope.setState('stop')
        }
    }

    $scope.setState = function(statusKey, event) {
        console.debug('status ' + statusKey)

        $scope.service.status = statusKey

        switch (statusKey) {
        case 'onstart':
            $scope.service.isListening = true
            $scope.service.img = '../WebkitSpeechRecognition/mic-animate.gif'
            // $scope.errorText = null
            $scope.startTimestamp = new Date().getTime()
            console.debug('speak now')
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
                    $scope.log.unshift($scope.recognizedResult)
                    $scope.$apply()
                } else {
                    // weird handling of this ... FIXME - just pubish it all and set the correct final ..
                    $scope.interimTranscript += data.transcript
                }
            }
            break

        case 'onend':
            $scope.service.isListening = false
            $scope.service.img = '../WebkitSpeechRecognition/mic-slash.png'
            if (!$scope.stopRequested) {
                $scope.restartCnt += 1
                recognizer.start()
            }
            break

        case 'onerror':
            $scope.errorText = event.error
            let errorTs = new Date().getTime()
            if ((errorTs - $scope.startTimestamp) < 100) {
                $scope.errorText += ' - high error rate - check other tabs for an active webkit speech recognizer, and close it'
                $scope.$apply()
            }
            if ($scope.errorText == 'no-speech') {
                console.debug('onerror - ' + $scope.errorText)
            } else {
                console.error('onerror - ' + $scope.errorText)
            }

            break

        case 'stop':
            // TODO - rename stopListeningRequest
            $scope.stopRequested = true
            recognizer.stop()
            break

        case 'start':
            $scope.stopRequested = false
            if ($scope.service.isListening) {
                recognizer.stop()
            }
            recognizer.lang = $scope.selectedLanguage
            recognizer.start()
            $scope.errorText = null

            break
        default:
            console.error("unhandled status " + statusKey)
            break
        }

    }

    if (!('webkitSpeechRecognition'in window)) {
        webkitSupport = false
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
        if ($scope.service.isListening) {
            recognizer.stop()
        }
    }

    this.updateState = function(service) {
        // $scope.service is old data
        // service is new data

        if ($scope.service.isListening && !service.isListening) {
            $scope.setState('stop')
        }
        if (!$scope.service.isListening && service.isListening) {
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
            $scope.properties = mrl.getProperties(data)
            $scope.$apply()
            break
        case 'onOnStartSpeaking':
            console.log("Started speaking, stop listening.")
            $scope.startRecognition()
            break
        case 'onOnEndSpeaking':
            console.log("Stopped speaking, start listening.")
            if (!$scope.service.isListening) {
                $scope.startRecognition()
            }
            break
        default:
            console.log("Unknown Message recieved." + msg.method)
            break
        }
    }

    // $scope.setState('start')

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
