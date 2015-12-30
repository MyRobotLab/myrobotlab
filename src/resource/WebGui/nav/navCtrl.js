angular.module('mrlapp.nav')
        .controller('navCtrl', ['$scope', '$log', '$filter', '$timeout', '$location', '$anchorScroll', '$uibModal', 'mrl', 'statusSvc', 'serviceSvc', 'noWorkySvc',
            function ($scope, $log, $filter, $timeout, $location, $anchorScroll, $uibModal, mrl, statusSvc, serviceSvc, noWorkySvc) {

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

                // load type ahead service types
                $scope.possibleServices = mrl.getPossibleServices();
                console.log('possibleServices', $scope.possibleServices);

                // get platform information for display
                var p = mrl.getPlatform();
//                var branch = (p.branch == 'master') ? '' : p.branch;
                $scope.platform = p.branch + " " + p.arch + "." + p.bitness + "." + p.os + " " + p.mrlVersion;

                // ==== status history begin ========= 
                // global callback for "all" service status
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
                // ==== status history end ========= 

                //START_Alerts
                $scope.alerts = [
//                    {type: 'danger', msg: 'Oh snap! Change a few things up and try submitting again.'},
//                    {type: 'success', msg: 'Well done! You successfully read this important alert message.'}
                ];

                $scope.addAlert = function (type, msg) {
                    $scope.alerts.push({
                        type: type,
                        msg: msg
                    });
                };

                $scope.closeAlert = function (index) {
                    $scope.alerts.splice(index, 1);
                };
                
                statusSvc.registerAddAlertCallback($scope.addAlert);
                //END_Alerts

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
                
                $scope.about = function () {
                    var modalInstance = $uibModal.open({
                        animation: true,
                        templateUrl: 'nav/about.html',
                        controller: 'aboutCtrl'
//                        size: 'sm',
//                        scope: $scope
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
