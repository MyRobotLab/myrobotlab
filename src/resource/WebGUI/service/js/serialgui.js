angular.module('mrlapp.service.serialgui', [])
        .controller('SerialGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                var _self = this;
                $log.info('SerialGuiCtrl');

                this.init = function () {
                    // initialization
                    $scope.data.rx = "";
                    $scope.data.rxCount = 0;
                    $scope.data.tx = "";
                    $scope.data.txCount = 0;
                    $scope.data.txData = "";
                    $scope.data.possiblePorts = [];
                    $scope.data.possibleBaud = ['600', '1200', '2400', '4800', '9600', '19200', '38400', '57600', '115200'];

                    $scope.data.updateState = function (service) {
                        $scope.data.service = service;
                        $scope.data.isConnected = ($scope.data.service.portName != null);
                        $scope.data.isConnectedImage = ($scope.data.service.portName != null) ? "connected" : "disconnected";
                        $scope.data.connectText = ($scope.data.service.portName == null) ? "connect" : "disconnect";
                    };



                    $scope.data.dynamicPopover = {
                        content: 'Hello, World!',
                        templateUrl: 'myPopoverTemplate2.html',
                        title: 'Title'
                    };

                    // initial update
                    $scope.data.service = this.getService();
                    $scope.data.updateState($scope.data.service);

                    this.onMsg = function (msg) {
                        $log.info('CALLBACK - ' + msg.method);
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
                            case 'onRX':
                                $scope.data.rx += ' ' + msg.data[0];
                                ++$scope.data.rxCount;
                                if ($scope.data.rx.length > 400) {
                                    $scope.data.rx = $scope.data.rx.substring($scope.data.rx.length - 400);
                                }
                                $scope.$apply();
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


                    //you can subscribe to methods
                    this.subscribe('getPortNames');
//                    this.subscribe('publishRX'); testing ...
                    this.subscribe('publishTX');
                    this.subscribe('publishState');
                    this.subscribe('refresh');

                    this.send('publishState');
                    this.send('getPortNames');

                    $scope.datarefresh = function () {
                        _self.send('refresh');
                    };

                    $scope.data.disconnect = function () {
                        _self.send('disconnect');
                    };

                    $scope.data.writeString = function (txData) {
                        _self.send('writeString', txData);
                    };

                    $scope.data.connect = function (portName, baudrate, databits, stopbits, parity) {
                        _self.sendTo('connect', portName, baudrate, databits, stopbits, parity);
                    };
                };

                $scope.cb.notifycontrollerisready(this);
            }]);
