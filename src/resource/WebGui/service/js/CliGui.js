angular.module('mrlapp.service.CliGui', [])
        .controller('CliGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('CliGuiCtrl');
                var _self = this;
                var msg = this.msg;
                
                this.updateState = function (service) {
                    $scope.service = service;
                };
                _self.updateState($scope.service);

                this.onMsg = function (inMsg) {
                    switch (inMsg.method) {
                        case 'onStdout':
                            $scope.cli = $scope.cli + '\n' + inMsg.data[0];
                            $scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + inMsg.method);
                            break;
                    }
                };

                //$scope.myStyle = ".boxsizingBorder {-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;}";    
                // $scope.myStyle = "{'background-color':'black';}";

                var buffer = "";

                $scope.keyPress = function (event) {
                    var keyCode = event.keyCode;
                    var c = String.fromCharCode((96 <= keyCode && keyCode <= 105) ? keyCode - 48 : keyCode);
                    $log.info('keyPress ', keyCode);
                    if (keyCode == 13) {
                        mrl.sendTo(name, 'process', buffer);
                        buffer = '';
                        return;
                    }
                    buffer = buffer + String.fromCharCode(keyCode);
                };

                mrl.subscribe($scope.service.name, 'stdout');

                msg.subscribe(this);
            }
        ]);
