angular.module('mrlapp.service.pythongui', [])
        .controller('PythonGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                var _self = this;
                $log.info('PythonGuiCtrl');

                this.init = function () {
                    // get latest copy of a service
                    $scope.data.service = this.getService();
                    //init some variables
                    $scope.data.output = '';
                    // the awesome ace editor 1
                    $scope.data.editor = null;

                    this.onMsg = function (msg) {
                        switch (msg.method) {
                            case 'onStdOut':
                                $scope.data.output = msg.data[0] + $scope.data.output;
                                // $scope.data.output = $scope.data.output + msg.data[0];
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
                    };

                    $scope.data.aceLoaded = function (e) {
                        $log.info("ace loaded");
                        // Options
                        $scope.data.editor = e;
                        //editor.setReadOnly(true);
                    };

                    $scope.data.aceChanged = function (e) {
                        $log.info("ace changed");
                        //
                    };

                    $scope.data.execute = function () {
                        $log.info("execute");
                        _self.send("exec", $scope.data.editor.getValue());
                    };

                    $scope.data.stop = function () {
                    };

                    $scope.data.save = function () {
                    };

                    $scope.data.copy = function () {
                    };

                    //Subscriptions
                    this.subscribe('publishStdOut');
                    this.subscribe('clockStarted');
                    this.subscribe('clockStopped');

                    // FIXME re-entrant?
                    this.send("attachPythonConsole");
                };

                $scope.cb.notifycontrollerisready(this);
            }]);
