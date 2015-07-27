angular.module('mrlapp.nav')

        .controller('NavCtrl', ['$scope', '$log', '$filter', '$timeout', '$location', '$anchorScroll', 'mrl', 'StateSvc', 'ServiceSvc',
            function ($scope, $log, $filter, $timeout, $location, $anchorScroll, mrl, StateSvc, ServiceSvc) {

                //START_green-/red-LED
                // TODO - green png if connected - if not re-connect button
                if (mrl.isConnected()) {
                    $scope.connected = 'connected';
                } else {
                    $scope.connected = 'disconnected';
                }

                var onOpen = function () {
                    $scope.$apply(function () {
                        $scope.connected = 'connected';
                    });
                };

                var onClose = function () {
                    $scope.$apply(function () {
                        $scope.connected = 'disconnected';
                    });
                };

                mrl.subscribeOnOpen(onOpen);
                mrl.subscribeOnClose(onClose);
                //END_green-/red-LED

                //START_Status
                $scope.statuslist = StateSvc.getStatuses();

                // FIXME change class not style here ! uniform danger/error/warn/info
                // FIXME -> if error pink background
//        $scope.statusStyle = "statusStyle={'background-color':'pink'}";

                var onStatus = function (statusMsg) {
                    var s = statusMsg.data[0];
                    console.log('status', s);
                    $scope.$apply(function () {
                        StateSvc.addStatus(s.name, s.level, s.detail);
                        //TODO - think of a better solution (instead of firststatus) (and hopefully better styled)
                        var status = $scope.statuslist[$scope.statuslist.length - 1];
                        $scope.firststatus = status.name + " " + status.level + " " + status.detail;
                    });
                };

                mrl.subscribeToMethod(onStatus, "onStatus");
                //END_Status

                $scope.about = function () {
                    // modal display of all contributors & link to myobotlab.org
                    // & version & platform
                    $log.info('about');
                };

                $scope.help = function () {
                    // modal display of no worky 
                    $log.info('help');
                };

                //START_minimize/expand all & minlist
                $scope.showAll = function (show) {
                    //minimize / expand all panels
                    $log.info('showAll', show);
                    ServiceSvc.showAll(show);
                };

                $scope.showminlist = false;
                $scope.toggleMinList = function () {
                    $log.info('toggling MinList');
                    $scope.showminlist = !$scope.showminlist;
                };

                //service-panels & update-routine (also used for search)
                var panelsUpdated = function () {
                    $timeout(function () {
                        $scope.allpanels = ServiceSvc.getPanelsList();
                        $scope.minlist = $filter('panellist')($scope.allpanels, 'min');
                    });
                };
                panelsUpdated();
                ServiceSvc.subscribeToUpdates(panelsUpdated);
                //END_minimize/expand all & minlist

                //START_Search
                //panels are retrieved above (together with minlist)
                $log.info('searchPanels', $scope.allPanels);

                $scope.searchOnSelect = function (item, model, label) {
                    $log.info('searchOnSelect');
                    //scroll to selected panel
                    $location.hash(item.name + '_-_' + item.panelname + '_-_');
                    $anchorScroll();
                };
                //END_Search
            }]);
