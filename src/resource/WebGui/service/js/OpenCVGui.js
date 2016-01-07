angular.module('mrlapp.service.OpenCVGui', [])
        .controller('OpenCVGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('OpenCVGuiCtrl');
                var _self = this;
                var msg = this.msg;

                // GOOD TEMPLATE TO FOLLOW
                this.updateState = function (service) {
                    $scope.service = service;
                    $log.info("State had been updated");
                    $log.info(service);
                    
                };
                _self.updateState($scope.service);

                // init scope variables
                $scope.cameraIndex = 1;
                $scope.startCaptureLabel = "Start Capture";
                $scope.frameGrabber = "VideoInput";
                $scope.selectFrameGrabber = function selectFrameGrabber(frameGrabber) {
                	$log.info("Updating Frame Grabber ");
                	$scope.frameGrabber = frameGrabber;
                	mrl.sendTo($scope.service.name, "setFrameGrabberType" , frameGrabber);                	
                }
                
                $scope.selectCameraIndex = function(cameraIndex) {
                	$log.info("Updating Camera Index ..." + cameraIndex);
                	$scope.cameraIndex = cameraIndex;
                	mrl.sendTo($scope.service.name, "setCameraIndex", cameraIndex);
                }
                
                // start capture button click.
                $scope.startCapture = function() {
                	// send a message to open cv servce to start capture.
                	$log.info("Start capture clicked.");
                	$scope.service = mrl.getService($scope.service.name);
                	// mrl.sendTo($scope.service.name, "setCameraIndex", cameraIndex);
                	if ($scope.startCaptureLabel === "Stop Capture") {
                		mrl.sendTo($scope.service.name, "stopCapture");
                        $scope.startCaptureLabel = "Start Capture";
                	} else {
                	  mrl.sendTo($scope.service.name, "capture");
                      $scope.startCaptureLabel = "Stop Capture";
                	}
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
                // msg.subscribe('publishDisplay');
                msg.subscribe(this);
            }
        ]);
