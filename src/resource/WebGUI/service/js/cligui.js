angular.module('mrlapp.service.cligui', [])
        .controller('CLIGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                var _self = this;
                $log.info('CLIGuiCtrl');

                this.init = function () {
                    // get fresh copy
                    var service = mrl.getService($scope.data.service.name);

                    this.onMsg = function (msg) {
                        switch (msg.method) {
                            case 'onStdout':
                                $scope.data.cli = $scope.data.cli + '\n' + msg.data[0];
                                $scope.$apply();
                                break;
                            case 'onClockStarted':
                                $scope.data.label = "Stop";
                                $scope.$apply();
                                break;
                            case 'onClockStopped':
                                $scope.data.label = "Start";
                                $scope.$apply();
                                break;
                            default:
                                $log.error("ERROR - unhandled method " + msg.method);
                                break;
                        }
                    }
                    ;

                    //$scope.myStyle = ".boxsizingBorder {-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;}";    
                    // $scope.myStyle = "{'background-color':'black';}";

                    var buffer = "";

                    $scope.data.keyPress = function (event) {
                        var keyCode = event.keyCode;
                        var c = String.fromCharCode((96 <= keyCode && keyCode <= 105) ? keyCode - 48 : keyCode);
                        $log.info('keyPress ', keyCode);
                        if (keyCode == 13) {
                            this.send('process', buffer);
                            buffer = '';
                            return;
                        }
                        buffer = buffer + String.fromCharCode(keyCode);
                    };

                    this.subscribe('stdout');
                };

                $scope.cb.notifycontrollerisready(this);
            }
        ]);
