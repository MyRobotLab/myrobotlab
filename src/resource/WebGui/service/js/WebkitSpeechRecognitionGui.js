angular.module('mrlapp.service.WebkitSpeechRecognitionGui', [])
        .controller('WebkitSpeechRecognitionGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
    $log.info('WebkitSpeechRecognitionGuiCtrl');

    this.updateState = function (service) {
        $scope.service = service;
    };

    var _self = this;
    var msg = this.msg;
    _self.updateState($scope.service);
    
    // when to use $scope or anything?!
    $scope.currResponse = '';
    $scope.utterance = '';
    $scope.username = 'default';
    $scope.current_text = '';
    // start info status
    $scope.status = 'Click on the microphone icon and begin speaking.';
    $scope.rows = [];
    // current state for the button i guess?
    $scope.recognizing = false;
    // always grab the right service!
    $scope.service = mrl.getService($scope.service.name);
    // webkit $scope.recognition google speech 
	$scope.recognition = new webkitSpeechRecognition();
	// config properties on the webkit speech stuff.
	$scope.recognition.continuous = true;
	$scope.recognition.interimResults = true;
	
	// called when $scope.recognition starts.
	$scope.recognition.onstart = function() {
		$scope.recognizing = true;
		$scope.status = 'Speak now.';
		$scope.listenbuttonimg = 'service/img/mic-animate.gif';
		$scope.$apply();
	};

	// called when there's an error (handles a few cases)
	$scope.recognition.onerror = function(event) {
		if (event.error == 'no-speech') {
		  $scope.listenbuttonimg = 'service/img/mic.gif';
	      $scope.status = 'No speech was detected. You may need to adjust your <a href="//support.google.com/chrome/bin/answer.py?hl=en&amp;answer=1407892">microphone settings</a>.';
	      ignore_onend = true;
	      $scope.$apply();
		};
		if (event.error == 'audio-capture') {
		  $scope.listenbuttonimg = 'service/img/mic.gif';
	      $scope.status = 'No microphone was found. Ensure that a microphone is installed and that<a href="//support.google.com/chrome/bin/answer.py?hl=en&amp;answer=1407892">microphone settings</a> are configured correctly.';
	      ignore_onend = true;
	      $scope.$apply();
		};
		if (event.error == 'not-allowed') {
	      if (event.timeStamp - start_timestamp < 100) {
	    	  $scope.status = 'Permission to use microphone is blocked. To change, go to chrome://settings/contentExceptions#media-stream';
	    	  $scope.$apply();
	      } else {
	    	  $scope.status = 'Permission to use microphone was denied.';
	    	  $scope.$apply();
	      };
	      ignore_onend = true;
		};
	};

	// called when $scope.recognition finishes.
	$scope.recognition.onend = function() {
		$scope.recognizing = false;
		$scope.$apply();
		if (ignore_onend) {
			return;
		};
		$scope.listenbuttonimg = 'service/img/mic.gif';
		$scope.$apply();
		if (!final_transcript) {
			$scope.status = 'Click on the microphone icon and begin speaking.';
			$scope.$apply();
			return;
		};
		
	};
	
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
				
				final_transcript  = '';
				interm_transcript = '';
			} else {
		    	// we're not at the boundry of speech detection
				// append this fragment. 
				interim_transcript += event.results[i][0].transcript;
			};
		};
		$scope.current_text = interim_transcript;
		$scope.$apply();
		if (final_transcript || interim_transcript) {
			// TODO: fix this? do we care?
			$log.info("show buttons should be called here for inline-block");
		};
	}; 

    // toggle type of button for starting/stopping speech $scope.recognition.
	$scope.startRecognition = function () {
		$log.info("Start Recognition clicked.");
		if ($scope.recognizing) {
			$log.info("Stoppping recognition");
			$scope.recognition.stop();
			return;
		};
		// init the current text.
		final_transcript = '';
		$scope.recognition.lang = select_dialect.value;
		// start the recognizer
		$scope.recognition.start();
		ignore_onend = false;
		$scope.listenbuttonimg = 'service/img/mic-slash.gif'; 
		// start_img.src = 'service/img/mic-slash.gif';
		$scope.status = 'Click the "Allow" button above to enable your microphone.';
		start_timestamp = event.timeStamp;
		// $scope.$apply();
	};

	
	this.onMsg = function (msg) {
        $log.info("Webkit Speech Msg !");
        $log.info(msg.method);
        switch (msg.method) {
          case 'onState':
            _self.updateState(msg.data[0]);
            $scope.$apply();
            break;
          case 'onOnStartSpeaking':
        	$log.info("Started speaking, pausing listening.");
        	$scope.startRecognition();
        	break;
          case 'onOnEndSpeaking':
        	$log.info("Stopped speaking, resume listening.");
        	if (!$scope.recognizing) {
          	  $scope.startRecognition();
            }
        	break;
        };
    }; 
	
	msg.subscribe('onStartSpeaking');
	msg.subscribe('onEndSpeaking');
	msg.subscribe(this);
    
}]);