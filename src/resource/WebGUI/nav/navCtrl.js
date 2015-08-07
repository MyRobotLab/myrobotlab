angular.module('mrlapp.nav')
.controller('NavCtrl', ['$scope', '$log', '$filter', '$location', '$anchorScroll', 'mrl', 'StateSvc', 'serviceSvc', 
function($scope, $log, $filter, $location, $anchorScroll, mrl, StateSvc, serviceSvc) {
    
    //START_green-/red-LED
    // TODO - green png if connected - if not re-connect button
    if (mrl.isConnected()) {
        $scope.connected = 'connected';
    } else {
        $scope.connected = 'disconnected';
    }
    
    var onOpen = function() {
        $scope.$apply(function() {
            $scope.connected = 'connected';
        }
        );
    }
    ;
    
    var onClose = function() {
        $scope.$apply(function() {
            $scope.connected = 'disconnected';
        }
        );
    }
    ;
    
    mrl.subscribeOnOpen(onOpen);
    mrl.subscribeOnClose(onClose);
    //END_green-/red-LED
    
    //START_Status
    // FIXME - what does StateSvc have statuses GAH names all wrong    
    $scope.statuslist = StateSvc.getStatuses();
    
    $scope.possibleServices = mrl.getPossibleServices();
    
    var p = mrl.getPlatform();
    $scope.platform = p.arch + "." + p.bitness + "." + p.os + " " + p.mrlVersion;
    
    // FIXME change class not style here ! uniform danger/error/warn/info
    // FIXME -> if error pink background
    //        $scope.statusStyle = "statusStyle={'background-color':'pink'}";
    
    var onStatus = function(statusMsg) {
        var s = statusMsg.data[0];
        $log.info('status', s);
        $scope.$apply(function() {
            StateSvc.addStatus(s);
        }
        );
    }
    ;
    
    mrl.subscribeToMethod(onStatus, "onStatus");
    //END_Status
    
    $scope.about = function() {
        // modal display of all contributors & link to myobotlab.org
        // & version & platform
        $log.info('about');
    }
    ;
    
    $scope.help = function() {
        // modal display of no worky 
        $log.info('help');
    }
    ;
    
    $scope.hideAll = function() {
        $log.info('help');
        serviceSvc.hideAll();
    }
    ;
    
    $scope.showAll = function() {
        // show all panels
        $log.info('showAll');
        serviceSvc.showAll();
    }
    ;
    
    $scope.test = function() {
        $log.info('test');
        var x = mrl.getPossibleServices();
    }
    ;
    
    $scope.showminlist = false;
    $scope.toggleMinList = function() {
        $log.info('toggling MinList');
        $scope.showminlist = !$scope.showminlist;
    }
    ;
    
    
    //START_Search
    // $scope.searchPanels = mrl.getServices();
    $scope.searchPanels = serviceSvc.getPanelList();
    $log.info('searchPanels', $scope.searchPanels);
    
    $scope.searchOnSelect = function(item, model, label) {
        
        serviceSvc.setPosition(item.name, 0, 0);
        // FIXME - check if panel is hide or show - make hide
        // FIXME - change position to (0, 0) with highest z index
        //scroll to selected service
        // $location.hash(item.name);
        // $anchorScroll();
    }
    ;
    
    $scope.start = function(newName, newTypeModel) {
        mrl.sendTo(mrl.getRuntime().name, "start", newName, newTypeModel.name);
    }

}
]);
