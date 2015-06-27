angular.module('mrlapp.nav')

.controller('NavCtrl', ['$scope', '$location', '$anchorScroll', 'StateSvc', 'mrl', 
    function($scope, $location, $anchorScroll, StateSvc, mrl) {
        
        var onOpen = function() {
            $scope.$apply(function() {
                $scope.connected = 'connected';
            });
        }
        
        var onClose = function() {
            $scope.$apply(function() {
                $scope.connected = 'disconnected';
            });
        }

        var onStatus = function(status) {
            $scope.$apply(function() {
               StateSvc.addStatus(status);
            });
        }

        $scope.about = function() {
            // modal display of all contributors & link to myobotlab.org
            console.log('about');
        };
        
        $scope.help = function() {
            // modal display of no worky 
            console.log('help');
        };

        //TODO: find a way to get all Services - probably something like mrl.getAllServices()
        $scope.searchServices = [];
        
        // TODO - green png if connected - if not re-connect button
        if (mrl.isConnected()) {
            $scope.connected = 'connected';
        } else {
            $scope.connected = 'disconnected';
        }
        
        $scope.searchOnSelect = function(item, model, label) {
            console.log('searchOnSelect');
            //scroll to selected service
            $location.hash(item.name);
            $anchorScroll();
        };

        // FIXME change class not style here ! uniform danger/error/warn/info
        // FIXME -> if error pink background
        $scope.statusStyle = "statusStyle={'background-color':'pink'}";

        mrl.subscribeOnOpen(onOpen);
        mrl.subscribeOnClose(onClose);
        mrl.subscribeToMethod(onStatus, "onStatus");

        $scope.statuslist = StateSvc.getStatuses();
        
        /*
        StateSvc.addStatus('And this is my status history!');
        StateSvc.addStatus('And this is my status history!');
        StateSvc.addStatus('And this is my status history!');
        StateSvc.addStatus('I am going to be the new WebUI for MyRobotLab!');
        */

    }]);
