angular.module('mrlapp.nav')
        .controller('navCtrl', ['$scope', '$log', '$filter', '$timeout', '$location', '$anchorScroll', '$state', '$uibModal', 'mrl', 'statusSvc', 'serviceSvc', 'noWorkySvc','Flash',
            function ($scope, $log, $filter, $timeout, $location, $anchorScroll, $state, $uibModal, mrl, statusSvc, serviceSvc, noWorkySvc, Flash) {

                //connection state LED
                $scope.connected = mrl.isConnected();
                mrl.subscribeConnected(function (connected) {
                    $log.info('nav:connection update', connected);
                    $timeout(function () {
                        $scope.connected = connected;
                    });
                });

                // load type ahead service types
                $scope.possibleServices = mrl.getPossibleServices();

                // get platform information for display
                $scope.platform = mrl.getPlatform();

                // status info warn error
                $scope.statusList = statusSvc.getStatuses();
                statusSvc.subscribeToUpdates(function (status) {
                    $timeout(function () {
                        // $scope.firststatus = status.name + " " + status.level + " " + status.detail;
                        $scope.status = status;
                        // var id = Flash.create('success', status.level + ' ' + status.name + ' ' + status.detail + ' ', 0, {class: 'custom-class', id: 'custom-id'}, true);
                    });
                });

                $scope.showAll = serviceSvc.showAll;
                $scope.showminlist = false;

                //service-panels & update-routine (also used for search)
                var panelsUpdated = function (panels) {
                    $scope.allpanels = panels;
                    $timeout(function () {
                        $scope.minlist = $filter('panellist')($scope.allpanels, 'min');
                    });
                };
                panelsUpdated(serviceSvc.getPanelsList());
                serviceSvc.subscribeToUpdates(panelsUpdated);

                $scope.shutdown = function (type) {
                    var modalInstance = $uibModal.open({
                        animation: true,
                        templateUrl: 'nav/shutdown.html',
                        controller: 'shutdownCtrl',
                        resolve: {
                            type: function () {
                                return type;
                            }
                        }
                    });
                };

                $scope.about = function () {
                    var modalInstance = $uibModal.open({
                        animation: true,
                        templateUrl: 'nav/about.html',
                        controller: 'aboutCtrl'
                    });
                };

                $scope.help = function () {
                    // should be something with help - for now: no Worky
                    //-> maybe tipps & tricks, ...
                    noWorkySvc.openNoWorkyModal('');
                };

                $scope.noWorky = function () {
                    // modal display of no worky 
                    noWorkySvc.openNoWorkyModal('');
                };

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
                    $scope.searchSelectedPanel = '';
                };
                //END_Search

                //quick-start a service
                $scope.start = function (newName, newTypeModel) {
                    mrl.sendTo(mrl.getRuntime().name, "start", newName, newTypeModel.name);
                    $scope.newName = '';
                    $scope.newType = '';
                };

                $scope.stateGo = $state.go;
            }]);
