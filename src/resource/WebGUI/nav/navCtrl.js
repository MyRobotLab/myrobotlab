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

                $scope.possibleServices = mrl.getPossibleServices();

                var p = mrl.getPlatform();
                $scope.platform = p.arch + "." + p.bitness + "." + p.os + " " + p.mrlVersion;

                // FIXME change class not style here ! uniform danger/error/warn/info
                // FIXME -> if error pink background
                //        $scope.statusStyle = "statusStyle={'background-color':'pink'}";

                var onStatus = function (statusMsg) {
                    var s = statusMsg.data[0];
                    $scope.$apply(function () {
                        StateSvc.addStatus(s);
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

                $scope.showAll = function (value) {
                    //hide or show all panels
                    $log.info('showAll', value);
                    ServiceSvc.showAll(value);
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

                //START_Search
                //panels are retrieved above (together with minlist)
                $log.info('searchPanels', $scope.allpanels);

                $scope.searchOnSelect = function (item, model, label) {
                    //expand panel if minified
                    if (item.list == 'min') {
                        item.panelsize.aktsize = item.panelsize.oldsize;
                        ServiceSvc.movePanelToList(item.name, item.panelname, 'main');
                    }
                    //show panel if hidden
                    if (item.hide) {
                        item.hide = false;
                    }
                    //put panel on top
                    ServiceSvc.putPanelZIndexOnTop(item.name, item.panelname);
                    item.notifyZIndexChanged();
                    //move panel to top of page
                    item.posx = 15;
                    item.posy = 0;
                    item.notifyPositionChanged();
                    //scroll to selected service
//                    $location.hash(item.name + '_' + item.panelname);
//                    $anchorScroll();
                };
                //END_Search

                $scope.start = function (newName, newTypeModel) {
                    mrl.sendTo(mrl.getRuntime().name, "start", newName, newTypeModel.name);
                };
            }]);
