angular.module('mrlapp.service.servogui', [])
        .controller('ServoGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                var _self = this;
                $log.info('ServoGuiCtrl');

                this.init = function () {
                    // Get a fresh copy!
                    $scope.data.service = this.getService();
                    $scope.data.controller = '';
                    $scope.data.pin = '';
                    $scope.data.min = 0;
                    $scope.data.max = 180;
                    $scope.data.angle = 0;
                    $scope.data.isAttached = $scope.data.service.isAttached;

                    // get and initalize current state of servo
                    $scope.data.attachButtonLabel = "Attach";
                    $scope.data.status = "No Status";
                    $scope.$apply();

                    this.onMsg = function (msg) {
                        $log.info("SERVO MSG: " + msg);
                        switch (msg.method) {
                            case 'onState':
                                $scope.data.status = msg.data[0];
                                $scope.data.isAttached = $scope.data.status.isAttached;
                                $scope.data.angle = $scope.data.status.angle;
                                $scope.data.min = $scope.data.status.outputYMin;
                                $scope.data.max = $scope.data.status.outputYMax;
                                if ($scope.data.isAttached === true) {
                                    $scope.data.attachButtonLabel = "Detach";
                                } else {
                                    $scope.data.attachButtonLabel = "Attach";
                                }
                                $scope.$apply();
                                break;
                            case 'onServoEvent':
                                $scope.data.status = msg.data[0];
                                $scope.$apply();
                                break;
                            case 'onStatus':
                                $scope.data.status = msg.data[0];
                                $scope.$apply();
                                break;
                            case 'addListener':
                                $log.info("Add listener called");
                                $scope.data.status = msg.data[0];
                                $scope.$apply();
                                break;
                            default:
                                $scope.data.status = msg.data[0];
                                $scope.$apply();
                                $log.info("ERROR - unhandled method " + $scope.data.service.name + " Method " + msg.method);
                                break;
                        }
                    };

                    $scope.data.attachDetach = function (controller, pin) {
                        if ($scope.data.status.isAttached === true) {
                            $log.info("Detach Servo");
                            _self.send("detach");
                            $scope.data.attachButtonLabel = "Detach";
                            $scope.$apply();
                        } else {
                            $log.info("Attach Servo");
                            _self.send("attach", controller, pin);
                            $scope.data.attachButtonLabel = "Attach";
                            $scope.$apply();
                        }
                    };

                    $scope.data.moveTo = function (angle) {
                        $log.info("Move TO " + angle);
                        this.send("moveTo", angle);
                    };

                    $scope.data.updateLimits = function (min, max) {
                        $log.info("Update Limits");
                        this.send("setMinMax", min, max);
                    };
                    // this.subscribe('publishServoEvent');  ??
                };
                $scope.cb.notifycontrollerisready(this);
            }]);
