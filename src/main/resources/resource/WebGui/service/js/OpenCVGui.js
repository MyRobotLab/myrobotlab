angular.module('mrlapp.service.OpenCVGui', [])
        .controller('OpenCVGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl, $sce) {
                $log.info('OpenCVGuiCtrl');
                // grab a refernce
                var _self = this;
                // grab the message
                var msg = this.msg;

                // local scope variables
                $scope.cameraIndex = 1;
                $scope.frameGrabber = "VideoInput";

                // initial state of service.
                $scope.service = mrl.getService($scope.service.name);
                if ($scope.service.capturing) {
                	$scope.startCaptureLabel = "Stop Capture";
                	// $sce.trustAsResourceUrl ?
                	$scope.imgSource = "http://localhost:9090/input";
                } else {
                	$scope.startCaptureLabel = "Start Capture";
                	$scope.imgSource = "service/img/opencv.png";
                }
                 
                // Handle an update state call from OpenCV service.
                this.updateState = function (service, $sce) {
                    $scope.service = service;
                    $log.info("Open CV State had been updated");
                    $log.info(service);
                    if ($scope.service.capturing) {
                    	$log.info("Started capturing");
                    	$scope.startCaptureLabel = "Stop Capture";
                    	$scope.imgSource = "http://localhost:9090/input";
                    } else {
                    	$log.info("Stopped capturing.");
                    	$scope.startCaptureLabel = "Start Capture";	
                    	$scope.imgSource = "service/img/OpenCV.png";
                    };
                };
                _self.updateState($scope.service);

                // controls for select frame grabber
                $scope.selectFrameGrabber = function selectFrameGrabber(frameGrabber) {
                	$log.info("Updating Frame Grabber ");
                	$scope.frameGrabber = frameGrabber;
                	mrl.sendTo($scope.service.name, "setFrameGrabberType" , frameGrabber);                	
                }
                
                // controls for select frame grabber                
                $scope.selectCameraIndex = function(cameraIndex) {
                	$log.info("Updating Camera Index ..." + cameraIndex);
                	$scope.cameraIndex = cameraIndex;
                	mrl.sendTo($scope.service.name, "setCameraIndex", cameraIndex);
                }
                
                // start capture button click.
                $scope.startCapture = function() {
                	// send a message to open cv servce to start capture.
                	$log.info("Start capture clicked.");
                	// TODO: should i grab it here?
                	$scope.service = mrl.getService($scope.service.name);
                	if ($scope.startCaptureLabel === "Stop Capture") {
                		// TODO: re-enable this.
                	    mrl.sendTo($scope.service.name, "stopCapture");
                	} else {
                	    mrl.sendTo($scope.service.name, "capture");
                	};
                };
                
                this.onMsg = function (inMsg) {
                    switch (inMsg.method) {
                        case 'onState':
                        	_self.updateState(inMsg.data[0]);
                            $scope.$apply();
                            break;
                        case 'onDisplay':
                            // $scope.pulseData = inMsg.data[0];
                            $scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
                            break;
                    }
                };
                // TODO: we're not going to publish the display.
                // we will start a video stream and update the page to display that stream.
                // mrl.subscribe($scope.service.name, 'publishState');
                msg.subscribe('getPossibleFilters');
                msg.subscribe('publishState');
                msg.subscribe(this);
            }
        ]);
