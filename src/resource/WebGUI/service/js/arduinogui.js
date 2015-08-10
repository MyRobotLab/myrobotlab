angular.module('mrlapp.service.arduinogui', [])
        .controller('ArduinoGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                var _self = this;
                $log.info('ArduinoGuiCtrl');

                this.init = function () {

                    this.onMsg = function (msg) {
                        //$log.info('CALLBACK - ' + msg.method);
                        switch (msg.method) {
                            case 'onPortNames':
                                $scope.data.possiblePorts = msg.data[0];
                                $scope.$apply();
                                break;
                            case 'onRefresh':
                                $scope.data.possiblePorts = msg.data[0];
                                $scope.$apply();
                                break;
                            case 'onState':
                                // backend update 
                                $scope.data.updateState(msg.data[0]);
                                $scope.$apply();
                                break;
                            case 'onPin':

                                break;
                            case 'onTX':
                                ++$scope.data.txCount;
                                $scope.data.tx += msg.data[0];
                                $scope.$apply();
                                break;
                            default:
                                $log.error("ERROR - unhandled method " + msg.method);
                                break;
                        }
                    };

                    $scope.data.service = this.getService();
                    $scope.data.showMRLComm = true;

                    $scope.data.updateState = function (service) {
                        $scope.data.service = service;
                        $scope.data.board = $scope.data.service.board;
                        $scope.data.isConnected = ($scope.data.service.portName != null);
                        $scope.data.isConnectedImage = ($scope.data.service.portName != null) ? "connected" : "disconnected";
                        $scope.data.connectText = ($scope.data.service.portName == null) ? "connect" : "disconnect";
                    };

                    // $scope display methods
                    $scope.data.onBoardChange = function (board) {
                        if ($scope.data.service.board != $scope.data.board) {
                            _self.send('setBoard', board);
                            _self.send('broadcastState');
                        }
                    };

                    $scope.data.toggleShowMRLComm = function () {
                        $scope.data.showMRLComm = ($scope.data.showMRLComm) ? false : true;
                    };

                    $scope.data.aceLoaded = function (editor) {
                        // Options
                        editor.setReadOnly(true);
                        editor.$blockScrolling = Infinity; //? Infinity ? undefined ?
                        editor.setValue($scope.data.service.sketch.data, -1);
                    };

                    $scope.data.aceChanged = function (e) {
                        //
                    };

                    // initial update
                    $scope.data.service = this.getService();
                    $scope.data.updateState($scope.data.service);


                    // subsumption !!! - we want to repress serial messages they are
                    // WAY TO MANY AND NOT INTEREsTING !!
                    // we want to unregister tx & rx events !!
                    // now the tricky part of finding "real" name of a peer ?
                    // mrl.getPeerName()
                    // this.send('unsubscribe', board);
                };

                $scope.cb.notifycontrollerisready(this);
            }]);
