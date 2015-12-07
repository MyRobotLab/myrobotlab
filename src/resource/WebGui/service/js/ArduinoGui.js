angular.module('mrlapp.service.ArduinoGui', [])
        .controller('ArduinoGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('ArduinoGuiCtrl');
                var _self = this;
                var msg = this.msg;

                $scope.version = "unknown";

                this.updateState = function (service) {
                    $scope.service = service;
                    //$scope.version = service.mrlCommVersion;
                };
                _self.updateState($scope.service);

                this.onMsg = function (inMsg) {  // THIS IS NOT GOOD !
                    //$log.info('CALLBACK - ' + inMsg.method);
                    switch (inMsg.method) {
                        case 'onPortNames':
                            $scope.possiblePorts = inMsg.data[0];
                            $scope.$apply();
                            break;
                        case 'onVersion':
                            $scope.version = inMsg.data[0];
                            $scope.$apply();
                            break;
                        case 'onRefresh':
                            $scope.possiblePorts = inMsg.data[0];
                            $scope.$apply();
                            break;
                        case 'onStatus':
                            // backend update 
                            $scope.updateState(inMsg.data[0]);
                            $scope.$apply();
                            break;
                        case 'onPin':

                            break;
                        case 'onTX':
                            ++$scope.txCount;
                            $scope.tx += inMsg.data[0];
                            $scope.$apply();
                            break;
                        default:
                            $log.error("ERROR - unhandled method " + inMsg.method);
                            break;
                    }
                };

                $scope.showMRLComm = true;

                $scope.updateState = function (service) {
                    $scope.service = service;
                    $scope.board = $scope.service.board;
                    $scope.isConnected = ($scope.service.portName != null);
                    $scope.isConnectedImage = ($scope.service.portName != null) ? "connected" : "disconnected";
                    $scope.connectText = ($scope.service.portName == null) ? "connect" : "disconnect";
                };

                // $scope display methods
                $scope.onBoardChange = function (board) {
                    if ($scope.service.board != $scope.board) {
                        mrl.sendTo(name, 'setBoard', board);
                        mrl.sendTo(name, 'broadcastState');
                    }
                };

                $scope.toggleShowMRLComm = function () {
                    $scope.showMRLComm = ($scope.showMRLComm) ? false : true;
                };

                $scope.aceLoaded = function (editor) {
                    // Options
                    editor.setReadOnly(true);
                    editor.$blockScrolling = Infinity;
                    editor.setValue($scope.service.sketch.data, -1);
                };

                $scope.aceChanged = function (e) {
                    //
                };

                // initial update
                $scope.service = mrl.getService($scope.service.name);
                $scope.updateState($scope.service);


                // subsumption !!! - we want to repress serial messages they are
                // WAY TO MANY AND NOT INTEREsTING !!
                // we want to unregister tx & rx events !!
                // now the tricky part of finding "real" name of a peer ?
                // mrl.getPeerName()
                // mrl.sendTo(name, 'unsubscribe', board);


                msg.subscribe('publishVersion');
                msg.send("getVersion");

                msg.subscribe(this);
            }]);
