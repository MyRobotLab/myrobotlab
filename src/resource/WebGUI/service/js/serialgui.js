angular.module('mrlapp.service.serialgui', [])
.controller('SerialGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
        console.log('SerialGuiCtrl');

        // get fresh copy
        $scope.service = mrl.getService($scope.service.name);

        //you can access two objects
        //$scope.gui & $scope.service
        //$scope.gui contains some framwork functions related to your service panel
        //-> you can call functions on it, but NEVER write in it
        //$scope.service is your service-object, it is the representation of the service running in mrl

        //with this method, you can set how many panels you would like to show
        $scope.gui.setPanelCount(1);

        //you HAVE TO define this method &
        //it is the ONLY exception of writing into .gui
        //-> you will receive all messages routed to your service here
        $scope.gui.onMsg = function(msg) {
            switch (msg.method) {
                case 'onPortNames':
                    $scope.possiblePorts = msg.data[0];
                    $scope.$apply();
                    break;
                case 'onClockStarted':
                    $scope.label = "Stop";
                    $scope.$apply();
                    break;
                case 'onClockStopped':
                    $scope.label = "Start";
                    $scope.$apply();
                    break;
                default:
                    console.log("ERROR - unhandled method " + msg.method);
                    break;
            }
        };

        //you can subscribe to methods
        mrl.subscribe($scope.service.name, 'getPortNames');
        mrl.sendTo($scope.service.name, 'getPortNames');

        $scope.port = '';//$scope.service.baudrate;
        $scope.possiblePorts = [];
        
        
        $scope.selected = $scope.service.baudrate;
        $scope.possibleBaud = ['600', '1200', '2400', '4800', '9600', '19200', '38400', '57600', '115200'];

        $scope.databits = $scope.service.databits;
        $scope.stopbits = $scope.service.stopbits;
        $scope.parity = $scope.service.parity;

        //after you're done with setting up your service-panel, call this method
        $scope.gui.initDone();
    }]);
