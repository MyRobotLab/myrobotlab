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
    let wakeWord = null

    $scope.restartCnt = 0
    $scope.interimTranscript = ''
    $scope.finalTranscript = ''
    $scope.publishedText = ''
    $scope.selectedLanguage = "en-US"
    $scope.startTimestamp = null
    $scope.stopRequested = false
    $scope.errorText = null;
    $scope.log = ''

    $scope.state = {
        recognizing: false,
        status: null,
        img: '../WebkitSpeechRecognition/mic.png',
        webkitSupport: true
    }

    $scope.setState = function(statusKey) {
        console.log('status ' + statusKey)
        $scope.state.status = statusKey
        switch (statusKey) {
        case 'onstart':
            $scope.state.recognizing = true
            $scope.state.img = '../WebkitSpeechRecognition/mic-animate.gif'
            // $scope.errorText = null
            $scope.startTimestamp = new Date().getTime()
            console.log('speak now')
            $scope.$apply()
            break
        case 'onend':
            $scope.state.recognizing = false
            $scope.state.img = '../WebkitSpeechRecognition/mic-slash.png'
            if (!$scope.stopRequested) {
                $scope.restartCnt += 1
                recognizer.start()
            }

            $scope.finalTranscript = $scope.finalTranscript.trim()
            $scope.log += ' ' + $scope.finalTranscript

            // publish results
            if ($scope.finalTranscript.length > 0) {
                $scope.publishedText = $scope.finalTranscript
                msg.send('publishRecognized', $scope.publishedText);
            }

            $scope.finalTranscript = ''
            $scope.$apply()
            break
        case 'onerror':
            console.error('onerror - ' + $scope.errorText)
            break
        case 'stop':
            $scope.stopRequested = true
            recognizer.stop()
            break
        case 'start':
            $scope.stopRequested = false
            if ($scope.state.recognizing) {
                recognizer.stop()
            }
            $scope.finalTranscript = ''
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
            $scope.setState('onerror')
            $scope.errorText = event.error
        }

        recognizer.onend = function() {
            $scope.setState('onend')
        }

        recognizer.onresult = function(event) {
            console.log('onresult')
            $scope.interimTranscript = ''
            for (var i = event.resultIndex; i < event.results.length; ++i) {
                if (event.results[i].isFinal) {
                    $scope.finalTranscript += event.results[i][0].transcript
                } else {
                    $scope.interimTranscript += event.results[i][0].transcript
                }
            }

            // final_span.innerHTML = $scope.finalTranscript
            // interim_span.innerHTML = $scope.interimTranscript
            if ($scope.finalTranscript || $scope.interimTranscript) {
                // showButtons('inline-block')
                console.log('inline-block')
            }
        }
    }

    $scope.setLanguage = function() {
        recognizer.lang = $scope.selectedLanguage
        if ($scope.state.recognizing) {
            recognizer.stop()
        }
    }

    this.updateState = function(service) {
        $scope.service = service
    }

    this.onMsg = function(msg) {
        console.log("webkit msg " + msg.method)
        switch (msg.method) {
        case 'onState':
            _self.updateState(msg.data[0])
            $scope.$apply()
            break
        case 'onOnStartSpeaking':
            console.log("Started speaking, stop listening.")
            $scope.startRecognition()
            break
        case 'onOnEndSpeaking':
            console.log("Stopped speaking, start listening.")
            if (!$scope.state.recognizing) {
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
    msg.subscribe(this)

}
])
