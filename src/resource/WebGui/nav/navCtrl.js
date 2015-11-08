angular.module('mrlapp.nav')
        .controller('navCtrl', ['$scope', '$log', '$filter', '$timeout', '$location', '$anchorScroll', 'mrl', 'statusSvc', 'serviceSvc',
            function ($scope, $log, $filter, $timeout, $location, $anchorScroll, mrl, statusSvc, serviceSvc) {

                //START_green-/red-LED
                $scope.connected = mrl.isConnected();

                mrl.subscribeOnOpen(function () {
                    $scope.$apply(function () {
                        $scope.connected = true;
                    });
                });
                mrl.subscribeOnClose(function () {
                    $scope.$apply(function () {
                        $scope.connected = false;
                    });
                });
                //END_green-/red-LED

                $scope.possibleServices = mrl.getPossibleServices();
                console.log('possibleServices', $scope.possibleServices);

                var p = mrl.getPlatform();
                $scope.platform = p.arch + "." + p.bitness + "." + p.os + " " + p.mrlVersion;

                //START_Status
                $scope.statuslist = statusSvc.getStatuses();

                //TODO would make sense to move this to serviceSvc - question is what happens with firststatus
                //don't think another notification-callback would be good
                var onStatus = function (statusMsg) {
//                    $timeout(function () {
                    statusSvc.addStatus(statusMsg.data[0]);
                    //TODO - think of a better solution (instead of firststatus) (and hopefully better styled)
                    var status = $scope.statuslist[$scope.statuslist.length - 1];
                    $scope.firststatus = status.name + " " + status.level + " " + status.detail;
//                    });
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
                    serviceSvc.showAll(value);
                };

                $scope.showminlist = false;

                //service-panels & update-routine (also used for search)
                var panelsUpdated = function (panels) {
                    $scope.allpanels = panels;
                    $timeout(function () {
                        $scope.minlist = $filter('panellist')($scope.allpanels, 'min');
                    });
                };
                panelsUpdated();
                serviceSvc.subscribeToUpdates(panelsUpdated);

                //START_Search
                //panels are retrieved above (together with minlist)
                $log.info('searchPanels', $scope.allpanels);

                $scope.searchOnSelect = function (item, model, label) {
                    //expand panel if minified
                    if (item.list == 'min') {
                        item.panelsize.aktsize = item.panelsize.oldsize;
                        serviceSvc.movePanelToList(item.name, item.panelname, 'main');
                    }
                    //show panel if hidden
                    if (item.hide) {
                        item.hide = false;
                    }
                    //put panel on top
                    serviceSvc.putPanelZIndexOnTop(item.name, item.panelname);
                    item.notifyZIndexChanged();
                    //move panel to top of page
                    item.posx = 15;
                    item.posy = 0;
                    item.notifyPositionChanged();
                    //scroll to selected service
//                    $location.hash(item.name + '_' + item.panelname);
//                    $anchorScroll();

                    $scope.searchSelectedPanel = '';
                };
                //END_Search

                //quick-start a service
                $scope.start = function (newName, newTypeModel) {
                    mrl.sendTo(mrl.getRuntime().name, "start", newName, newTypeModel.name);
                    $scope.newName = '';
                    $scope.newType = '';
                };
            }]);
