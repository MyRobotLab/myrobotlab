angular.module('mrlapp.service.clockgui', [])
        .controller('ClockGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                var _self = this;
                $log.info('ClockGuiCtrl');

                this.init = function () {
                    this.setPanelCount(2);

                    // get latest copy of a service
                    $scope.data.service = this.getService();
                    //init some variables
                    $scope.data.interval = $scope.data.service.interval;
                    $scope.data.isClockRunning = $scope.data.service.isClockRunning;
                    $scope.data.pulseData = '';


                    this.onMsg = function (msg) {
                        switch (msg.method) {
                            case 'onPulse':
                                $scope.data.pulseData = msg.data[0];
                                $scope.$apply();
                                break;
                            case 'onClockStarted':
                                $scope.data.isClockRunning = true;
                                $scope.$apply();
                                break;
                            case 'onClockStopped':
                                $scope.data.isClockRunning = false;
                                $scope.$apply();
                                break;
                            default:
                                $log.error("ERROR - unhandled method " + $scope.data.service.name + " " + msg.method);
                                break;
                        }
                    };

                    //start the clock
                    $scope.data.startClock = function (interval) {
                        console.log('send-this', this);
                        _self.send("setInterval", interval);
                        _self.send("startClock");
                    };

                    //stop the clock
                    $scope.data.stopClock = function () {
                        _self.send("stopClock");
                    };

                    //Subscriptions
                    this.subscribe('pulse');
                    this.subscribe('clockStarted');
                    this.subscribe('clockStopped');
                    //(still needs something for unsubscribing)
                };

                //this could be removed (and probably will, but was too much effort for now)
                $scope.cb.notifycontrollerisready(this);
            }]);
