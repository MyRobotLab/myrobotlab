angular.module('mrlapp.nav')

.controller('NavCtrl', ['$scope', '$location', '$anchorScroll', 'StateSvc', 'mrl', 'ServiceSvc',
    function($scope, $location, $anchorScroll, StateSvc, mrl, ServiceSvc) {
        
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
            });
        };
        
        var onClose = function() {
            $scope.$apply(function() {
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
        $scope.statusStyle = "statusStyle={'background-color':'pink'}";
        
        var onStatus = function(statusMsg) {
            var status = statusMsg.data[0];
            var s = status.name + ' ' + status.level + ' ' + status.detail;
            $scope.$apply(function() {
                StateSvc.addStatus(s);
            });
        };
        
        mrl.subscribeToMethod(onStatus, "onStatus");
        //END_Status

        $scope.about = function() {
            // modal display of all contributors & link to myobotlab.org
            console.log('about');
        };
        
        $scope.help = function() {
            // modal display of no worky 
            console.log('help');
        };

        //START_Search
        $scope.searchServices = ServiceSvc.getServices();
        console.log('searchServices', $scope.searchServices);
        
        $scope.searchOnSelect = function(item, model, label) {
            console.log('searchOnSelect');
            //scroll to selected service
            $location.hash(item.name + '_-_' + item.panelname + '_-_');
            $anchorScroll();
        };
        //END_Search
    }]);
