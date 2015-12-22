angular.module('mrlapp.nav')
        .controller('navCtrl', ['$scope', '$log', '$filter', '$timeout', '$location', '$anchorScroll', '$modal', 'mrl', 'statusSvc', 'serviceSvc',
            function ($scope, $log, $filter, $timeout, $location, $anchorScroll, $modal, mrl, statusSvc, serviceSvc) {
<<<<<<< HEAD
    
    $scope.noWorkyResult = "";
    $scope.noWorkyKey = "";
    $scope.noWorkyClass = "";
    
    //START_green-/red-LED
    $scope.connected = mrl.isConnected();
    
    mrl.subscribeOnOpen(function() {
        $scope.$apply(function() {
            $scope.connected = true;
        });
    });
    mrl.subscribeOnClose(function() {
        $scope.$apply(function() {
            $scope.connected = false;
        });
    });
    //END_green-/red-LED
    
    // load type ahead service types
    $scope.possibleServices = mrl.getPossibleServices();
    console.log('possibleServices', $scope.possibleServices);
    
    // get platform information for display
    var p = mrl.getPlatform();
    var branch = (p.branch == 'master') ? '' : p.branch;
    $scope.platform = p.branch + " " + p.arch + "." + p.bitness + "." + p.os + " " + p.mrlVersion;
    
    // ==== status history begin ========= 
    // global callback for "all" service status
    $scope.statuslist = statusSvc.getStatuses();
    
    //TODO would make sense to move this to serviceSvc - question is what happens with firststatus
    //don't think another notification-callback would be good
    var onStatus = function(statusMsg) {
        //                    $timeout(function () {
        statusSvc.addStatus(statusMsg.data[0]);
        //TODO - think of a better solution (instead of firststatus) (and hopefully better styled)
        var status = $scope.statuslist[$scope.statuslist.length - 1];
        $scope.firststatus = status.name + " " + status.level + " " + status.detail;
        //                    });
    }
    ;
    
    var onNoWorky = function(noWorkyResultssMsg) {
        var status = noWorkyResultssMsg.data[0];
        
        if (status.level == 'error') {
            //$log.here('here');class="alert alert-danger"
            // $timeout(onNoWorkySuccess, 1000);
            $scope.noWorkyClass = 'alert alert-danger';
            $scope.noWorkyResult = 'the noWorky did not worky !'
            $scope.noWorkyKey = status.key;
        } else {
            $scope.noWorkyClass = 'alert alert-success';
            $scope.noWorkyResult = 'noWorky sent !'
        }
=======
>>>>>>> branch 'develop' of https://github.com/MyRobotLab/myrobotlab.git

<<<<<<< HEAD
        $scope.$apply();
    
    }
    ;
    
    // subscriptions begin ===========
    // for history status
    mrl.subscribeToMethod(onStatus, "onStatus");
    // noWorky results
    // the manual setup since the navCtrl is not a serviceGui
    mrl.subscribeToServiceMethod(onNoWorky, mrl.getRuntime().name, 'publishNoWorky');
    // mrl.subscribeToMethod(onNoWorky, "onNoWorky");
    // the subscribe from Runtime to WebGUI
    mrl.subscribe(mrl.getRuntime().name, 'publishNoWorky');
    // ==== status history end ========= 
    
                   $scope.about = function () {
=======
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
>>>>>>> branch 'develop' of https://github.com/MyRobotLab/myrobotlab.git
                    $log.info('about');
                    var modalInstance = $modal.open({
                        animation: true,
                        templateUrl: 'nav/about.html',
                        controller: 'aboutCtrl',
//                        size: 'sm',
//                        scope: $scope
<<<<<<< HEAD
=======
                    });
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
>>>>>>> branch 'develop' of https://github.com/MyRobotLab/myrobotlab.git
                    });
                };
 
    $scope.help = function() {
        // modal display of no worky 
        $log.info('help');
    }
    ;
    
    $scope.showAll = function(value) {
        //hide or show all panels
        $log.info('showAll', value);
        serviceSvc.showAll(value);
    }
    ;
    
    $scope.showminlist = false;
    
    //service-panels & update-routine (also used for search)
    var panelsUpdated = function(panels) {
        $scope.allpanels = panels;
        $timeout(function() {
            $scope.minlist = $filter('panellist')($scope.allpanels, 'min');
        });
    }
    ;
    panelsUpdated();
    serviceSvc.subscribeToUpdates(panelsUpdated);
    
    //START_Search
    //panels are retrieved above (together with minlist)
    $log.info('searchPanels', $scope.allpanels);
    
    $scope.searchOnSelect = function(item, model, label) {
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
    }
    ;
    //END_Search
    
    
    $scope.noWorky = function(userId) {
        $scope.noWorkyResult = "";
        $scope.noWorkyKey = "";
        $scope.noWorkyClass = "";
        
        mrl.noWorky(userId);
        //$scope.modalShown = false;
    }
    ;
    
    // FIXME - make modal with passed in templateURL
    // e.g. modal(templateURL)
    $scope.modalShown = false;
    $scope.toggleModal = function() {
        $scope.modalShown = !$scope.modalShown;
    }
    ;
    
    
    //quick-start a service
    $scope.start = function(newName, newTypeModel) {
        mrl.sendTo(mrl.getRuntime().name, "start", newName, newTypeModel.name);
        $scope.newName = '';
        $scope.newType = '';
    }
    ;





}
]);
