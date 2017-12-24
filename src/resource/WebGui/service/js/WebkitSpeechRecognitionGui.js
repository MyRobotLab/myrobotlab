angular.module('mrlapp.service.WebkitSpeechRecognitionGui', [])
.controller('WebkitSpeechRecognitionGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('WebkitSpeechRecognitionGuiCtrl');
    
    
    var _self = this;
    var msg = this.msg;
    
    
    // Ok we want to have the language drop down.. and populate the 
    // possible languages here.
    // TODO: expand out the full set of langauges here.  (make it easy
    // the dialect & language merged as simple key/value pairs.
    // TODO: make sure on update we are setting this correctly?  perhaps
    // it's wrong to default it at all here.
    $scope.lang = {
        availableOptions: [
        {
            id: 'en-US',
            name: "English - United States"
        }, 
        {
            id: 'en-GB',
            name: "English - British"
        }, 
        {
            name: 'Afrikaans',
            id: 'af-ZA'
        }, 
        {
            name: 'Bahasa Indonesia',
            id: 'id-ID'
        }, 
        {
            name: 'Bahasa Melayu',
            id: 'ms-MY'
        }, 
        {
            name: 'Català',
            id: 'ca-ES'
        }, 
        {
            name: 'Čeština',
            id: 'cs-CZ'
        }, 
        {
            name: 'Dansk',
            id: 'da-DK'
        }, 
        {
            name: 'Deutsch',
            id: 'de-DE'
        }, 
        {
            name: 'English - Australia',
            id: 'en-AU'
        }, 
        {
            name: 'English - Canada',
            id: 'en-CA'
        }, 
        {
            name: 'English - India',
            id: 'en-IN'
        }, 
        {
            name: 'English - New Zealand',
            id: 'en-NZ'
        }, 
        {
            name: 'English - South Africa',
            id: 'en-ZA'
        }, 
        {
            name: 'English - United Kingdom',
            id: 'en-GB'
        }, 
        {
            name: 'English - United States',
            id: 'en-US'
        }, 
        {
            name: 'Español - Argentina',
            id: 'es-AR'
        }, 
        {
            name: 'Español - Bolivia',
            id: 'es-BO'
        }, 
        {
            name: 'Español - Chile',
            id: 'es-CL'
        }, 
        {
            name: 'Español - Colombia',
            id: 'es-CO'
        }, 
        {
            name: 'Español - Costa Rica',
            id: 'es-CR'
        }, 
        {
            name: 'Español - Ecuador',
            id: 'es-EC'
        }, 
        {
            name: 'Español - El Salvador',
            id: 'es-SV'
        }, 
        {
            name: 'Español - España',
            id: 'es-ES',
        }, 
        {
            name: 'Español - Estados Unidos',
            id: 'es-US'
        }, 
        {
            name: 'Español - Guatemala',
            id: 'es-GT'
        }, 
        {
            name: 'Español - Honduras',
            id: 'es-HN'
        }, 
        {
            name: 'Español - México',
            id: 'es-MX'
        }, 
        {
            name: 'Español - Nicaragua',
            id: 'es-NI'
        }, 
        {
            name: 'Español - Panamá',
            id: 'es-PA'
        }, 
        {
            name: 'Español - Paraguay',
            id: 'es-PY'
        }, 
        {
            name: 'Español - Perú',
            id: 'es-PE'
        }, 
        {
            name: 'Español - Puerto Rico',
            id: 'es-PR'
        }, 
        {
            name: 'Español - República Dominicana',
            id: 'es-DO'
        }, 
        {
            name: 'Español - Uruguay',
            id: 'es-UY'
        }, 
        {
            name: 'Español - Venezuela',
            id: 'es-VE'
        }, 
        {
            name: 'Euskara',
            id: 'eu-ES'
        }, 
        {
            name: 'Filipino',
            id: 'fil-PH'
        }, 
        {
            name: 'Français',
            id: 'fr-FR'
        }, 
        {
            name: 'Galego',
            id: 'gl-ES'
        },
        {
            name: 'Hindi - हिंदी',
            id: 'hi-IN'
        },         
        {
            name: 'Hrvatski',
            id: 'hr_HR'
        }, 
        {
            name: 'IsiZulu',
            id: 'zu-ZA'
        }, 
        {
            name: 'Íslenska',
            id: 'is-IS'
        }, 
        {
            name: 'Italiano - Italia',
            id: 'it-IT'
        }, 
        {
            name: 'Italiano - Svizzera',
            id: 'it-CH'
        }, 
        {
            name: 'Lietuvių',
            id: 'lt-LT'
        }, 
        {
            name: 'Magyar',
            id: 'hu-HU'
        }, 
        {
            name: 'Nederlands',
            id: 'nl-NL'
        }, 
        {
            name: 'Norsk bokmål',
            id: 'nb-NO'
        }, 
        {
            name: 'Polski',
            id: 'pl-PL'
        }, 
        {
            name: 'Português - Brasil',
            id: 'pt-BR'
        }, 
        {
            name: 'Português - Portugal',
            id: 'pt-PT'
        }, 
        {
            name: 'Română',
            id: 'ro-RO'
        }, 
        {
            name: 'Slovenščina',
            id: 'sl-SI'
        }, 
        {
            name: 'Slovenčina',
            id: 'sk-SK'
        }, 
        {
            name: 'Suomi',
            id: 'fi-FI'
        }, 
        {
            name: 'Svenska',
            id: 'sv-SE'
        }, 
        {
            name: 'Tiếng Việt',
            id: 'vi-VN'
        }, 
        {
            name: 'Türkçe',
            id: 'tr-TR'
        }, 
        {
            name: 'Ελληνικά',
            id: 'el-GR'
        }, 
        {
            name: 'български',
            id: 'bg-BG'
        }, 
        {
            name: 'Pусский',
            id: 'ru-RU'
        }, 
        {
            name: 'Српски',
            id: 'sr-RS'
        }, 
        {
            name: 'Українська',
            id: 'uk-UA'
        }, 
        {
            name: '한국어',
            id: 'ko-KR'
        }, 
        {
            name: '中文 - 普通话 (中国大陆)',
            id: 'cmn-Hans-CN'
        }, 
        {
            name: '中文 - 普通话 (香港)',
            id: 'cmn-Hans-HK'
        }, 
        {
            name: '中文 - 中文 (台灣)',
            id: 'cmn-Hant-TW'
        }, 
        {
            name: '中文 - 粵語 (香港)',
            id: 'yue-Hant-HK'
        }, 
        {
            name: '日本語',
            id: 'ja-JP'
        }, 
        {
            name: 'हिन्दी',
            id: 'hi-IN'
        }, 
        {
            name: 'ภาษาไทย',
            id: 'th-TH'
        }
        ],
        selectedOption: {
            id: 'en-US',
            name: "English - United States"
        }
    };
    continuous = true;
    clickedFromWebGui = false;
    this.updateState = function(service) {
        $scope.service = service;
        // we should check for the language here.. and update that on the webkit instance.
        // $scope.recognition.lang = service.language;
        // select_dialect.value = service.language;
        // TODO: update the selected value in the gui.
        $scope.lang.selectedOption.id = service.language;
        $scope.current_language = service.language;
        continuous = service.continuous;
        
        if ($scope.service.listening != $scope.recognizing) {
        	$log.info("Change listening state!");
        	// huh!? not initialized  yet?!
        	if (!angular.isUndefined($scope.recognizing)) {
        		// update the recognizing state of the gui.
        		// TODO: fix me. it doesn't handle the stop listening case (likely)
        		if ((!$scope.recognizing && $scope.service.listening)||!clickedFromWebGui) {
        			// TODO: start Recognition is actually a toggle.. not start.
        			$scope.startRecognition();
        		}
        	};
        } else {
        	$log.info("State did not change.");
        }
    };
    _self.updateState($scope.service);
    
    // when to use $scope or anything?!
    $scope.currResponse = '';
    $scope.utterance = '';
    $scope.current_text = '';
    // start info status
    $scope.status = 'Click on the microphone icon and begin speaking.[init]';
    $scope.rows = [];
    // current state for the button i guess?
    $scope.recognizing = false;
    // always grab the right service!
    $scope.service = mrl.getService($scope.service.name);
    // webkit $scope.recognition google speech
    // Check if webkitSpeechRecognition exists
    if (!('webkitSpeechRecognition' in window)){
    	$scope.wkavailable = false;
        $log.info('WebkitSpeechRecognition', 'not available');
        $scope.status = 'WebkitSpeechRecognition not available. Need to use Chrome on a supported platform.';
    }
    else
    {
    	$scope.wkavailable = true;
    	$scope.recognition = new webkitSpeechRecognition();
        // config properties on the webkit speech stuff.
        $scope.recognition.continuous = true;
        $scope.recognition.interimResults = true;
        mrl.sendTo($scope.service.name, "startListening");
        // called when $scope.recognition starts.
        $scope.recognition.onstart = function() {
        $scope.recognizing = true;
        $scope.status = 'Speak now.';
        $scope.listenbuttonimg = 'service/img/mic-animate.gif';
        $scope.$apply();
        mrl.sendTo($scope.service.name, "listeningEvent", "true");
    }
    };
    
    if ($scope.wkavailable){
    // called when there's an error (handles a few cases)
    $scope.recognition.onerror = function(event) {
        if (event.error == 'no-speech') {
            $scope.listenbuttonimg = 'service/img/mic.png';
            // force listen ? why not
            $scope.status = 'No speech was detected. You may need to adjust your <a href="//support.google.com/chrome/bin/answer.py?hl=en&amp;answer=1407892">microphone settings</a>.';
            ignore_onend = true;
            $scope.$apply();
            mrl.sendTo($scope.service.name, "startListening");
        }
        ;
        if (event.error == 'audio-capture') {
            $scope.listenbuttonimg = 'service/img/mic.png';
            $scope.status = 'No microphone was found. Ensure that a microphone is installed and that<a href="//support.google.com/chrome/bin/answer.py?hl=en&amp;answer=1407892">microphone settings</a> are configured correctly.';
            ignore_onend = true;
            $scope.$apply();
        }
        ;
        if (event.error == 'not-allowed') {
            if (event.timeStamp - start_timestamp < 100) {
                $scope.status = 'Permission to use microphone is blocked. To change, go to chrome://settings/contentExceptions#media-stream';
                $scope.$apply();
            } else {
                $scope.status = 'Permission to use microphone was denied.';
                $scope.$apply();
            }
            ;
            ignore_onend = true;
        }
        ;
    }
    ;
   
    // called when $scope.recognition finishes.
    $scope.recognition.onend = function() {
        mrl.sendTo($scope.service.name, "stopListening");
        $scope.recognizing = false;
        $scope.$apply();
        if (ignore_onend) {
            return;
        }
        ;
        $scope.listenbuttonimg = 'service/img/mic.png';
        $scope.$apply();
        if (!final_transcript) {
            $scope.status = 'Click on the microphone icon and begin speaking.[stop]';
            $scope.$apply();
            return;
        }
        ;
    
    }
    ;
    
    // called when a result is returned from $scope.recognition
    $scope.recognition.onresult = function(event) {
        // build up a string of the current utterance
        var interim_transcript = '';
        for (var i = event.resultIndex; i < event.results.length; ++i) {
            if (event.results[i].isFinal) {
                final_transcript += event.results[i][0].transcript;
                
                $scope.utterance = final_transcript;
                $scope.$apply();
                
                $log.info("Recognized Text Time to publish " + $scope.current_text);
                $scope.service = mrl.getService($scope.service.name);
                mrl.sendTo($scope.service.name, "recognized", $scope.current_text);
                mrl.sendTo($scope.service.name, "publishText", $scope.current_text);
                
                final_transcript = '';
                interm_transcript = '';
            } else {
                // we're not at the boundry of speech detection
                // append this fragment. 
                interim_transcript += event.results[i][0].transcript;
            }
            ;
        }
        ;
        $scope.current_text = interim_transcript;
        $scope.$apply();
        if (final_transcript || interim_transcript) {
            // TODO: fix this? do we care?
            $log.info("show buttons should be called here for inline-block");
        }
        ;
    }
    ;
    } // End of ($scope.wkavailable)
    
    
    $scope.updateLanguage = function() {
        $log.info('WEBKIT Update Language');
        // Here we need to update the language that we're recognizing.. and probably 
        // publish it back down to the java service.
        mrl.sendTo($scope.service.name, "setLanguage", $scope.lang.selectedOption.id);
    }
    
    // toggle type of button for starting/stopping speech $scope.recognition.
    $scope.startRecognition = function() {
        clickedFromWebGui = true;
        $log.info("Start Recognition clicked.");
        if ($scope.recognizing) {
            $log.info("Stoppping recognition");
            $scope.recognition.stop();
            return;
        }
        ;
        // init the current text.
        final_transcript = '';
        $scope.recognition.lang = $scope.lang.selectedOption.id;
        // start the recognizer
        $scope.recognition.start();
        ignore_onend = false;
        $scope.listenbuttonimg = 'service/img/mic-slash.png';
        // start_img.src = 'service/img/mic-slash.gif';
        $scope.status = 'Click the "Allow" button above to enable your microphone.';
        start_timestamp = event.timeStamp;
        // $scope.$apply();
    }
    ;
    
    
    this.onMsg = function(msg) {
        
        $scope.recognition.continuous = continuous;
        clickedFromWebGui = false;
        $log.info("Webkit Speech Msg !");
        $log.info(msg.method);
        switch (msg.method) {
        case 'onState':
            _self.updateState(msg.data[0]);
            $scope.$apply();
            break;
        case 'onOnStartSpeaking':
            $log.info("Started speaking, stop listening.");
            $scope.startRecognition();
            break;
        case 'onOnEndSpeaking':
            $log.info("Stopped speaking, start listening.");
            if (!$scope.recognizing) {
                $scope.startRecognition();
            }
            break;
        default:
        	$log.info("Unknown Message recieved." + msg.method);
            break;
        }
        ;
    }
    ;
    
    msg.subscribe('onStartSpeaking');
    msg.subscribe('onEndSpeaking');
    msg.subscribe('onStartListening');
    msg.subscribe('onStopListening');
    msg.subscribe(this);

}
]);