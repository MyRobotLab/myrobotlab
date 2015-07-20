angular.module('mrlapp.service.programabgui', [])
        .controller('ProgramABGuiCtrl', ['$scope', 'mrl', function ($scope, mrl) {
    console.log('ProgramABGuiCtrl');
    // when to use $scope or anything?!
    $scope.currResponse = '';
    $scope.utterance = '';
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
				$scope.askProgramAB(final_transcript);
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
			console.log("show buttons should be called here for inline-block");
		};
	}; 
    $scope.panel.onMsg = function (msg) {
        console.log("Program AB Msg !");
        if (msg.method == "onText") {
            var textData = msg.data[0];
            //$scope.serviceDirectory[msg.sender].pulseData = pulseData;
            $scope.currResponse = textData;
            $scope.rows.unshift({name:"Bot:" , response:textData});
            console.log('currResponse', $scope.currResponse);
            $scope.$apply();
        };
    }; 
    $scope.askProgramAB = function (utterance) {
    	$scope.service = mrl.getService($scope.service.name);
    	mrl.sendTo($scope.service.name, "getResponse", utterance);
    	$scope.rows.unshift({ name:"User" , response:utterance} );
    	// $scope.utterance = '';
    	// TODO: clear the text box.
    	// $scope.utterance = '';
    };
    $scope.startSession = function (botname,botpath) {
    	$scope.rows.unshift("Reload Session for Bot " + botname);
    	$scope.startSessionLabel = 'Reload Session';
    	$scope.$apply();
    	$scope.service = mrl.getService($scope.service.name);
    	mrl.sendTo($scope.service.name, "startSession", botpath, botname);
    }
    // toggle type of button for starting/stopping speech $scope.recognition.
	$scope.startRecognition = function () {
		console.log("Start Recognition clicked.");
		if ($scope.recognizing) {
			console.log("Stoppping recognition");
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
		$scope.$apply();
	};

    // subscribe to the response from programab.
    mrl.subscribe($scope.service.name, 'publishText');
    // we're done.
    $scope.panel.initDone(); 
    
  }]);