angular.module('mrlapp.service.serialgui', [])
.controller('SerialGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
        $log.info('SerialGuiCtrl');
        var _self = this;
        
        this.updateState = function(service) {
            $scope.service = service;
            $scope.isConnected = ($scope.service.portName != null);
            $scope.isConnectedImage = ($scope.service.portName != null) ? "connected" : "disconnected";
            $scope.connectText = ($scope.service.portName == null) ? "connect" : "disconnect";
        }

        // initialization
        $scope.rx = "";
        $scope.rxCount = 0;
        $scope.tx = "";
        $scope.txCount = 0;
        $scope.txData = "";
        $scope.possiblePorts = [];
        $scope.possibleBaud = ['600', '1200', '2400', '4800', '9600', '19200', '38400', '57600', '115200'];
        
        
        $scope.dynamicPopover = {
            content: 'Hello, World!',
            templateUrl: 'myPopoverTemplate2.html',
            title: 'Title'
        };

        // initial update
        $scope.service = mrl.getService($scope.service.name);
        _self.updateState($scope.service);

        //with this method, you can set how many panels you would like to show
        $scope.gui.setPanelCount(1);

        //you HAVE TO define this method &
        //it is the ONLY exception of writing into .gui
        //-> you will receive all messages routed to your service here
        // FIXME - why this function on the scope? why is it on gui ? - i believe it should be on this.onMsg
        $scope.gui.onMsg = function(msg) {
            $log.info('CALLBACK - ' + msg.method);
            switch (msg.method) {
                case 'onPortNames':
                    $scope.possiblePorts = msg.data[0];
                    $scope.$apply();
                    break;
                case 'onRefresh':
                    $scope.possiblePorts = msg.data[0];
                    $scope.$apply();
                    break;
                case 'onState':
                    // backend update 
                    _self.updateState(msg.data[0]);
                    $scope.$apply();
                    break;
                case 'onRX':
                    $scope.rx += ' ' + msg.data[0];
                    ++$scope.rxCount;
                    if ($scope.rx.length > 400) {
                        $scope.rx = $scope.rx.substring($scope.rx.length - 400);
                    }
                    $scope.$apply();
                    break;
                case 'onTX':
                    ++$scope.txCount;
                    $scope.tx += msg.data[0];
                    $scope.$apply();
                    break;
                default:
                    $log.error("ERROR - unhandled method " + msg.method);
                    break;
            }
        };

        //you can subscribe to methods
        mrl.subscribe($scope.service.name, 'getPortNames');
        mrl.subscribe($scope.service.name, 'publishRX');
        mrl.subscribe($scope.service.name, 'publishTX');
        mrl.subscribe($scope.service.name, 'publishState');
        mrl.subscribe($scope.service.name, 'refresh');
        
        mrl.sendTo($scope.service.name, 'publishState');
        mrl.sendTo($scope.service.name, 'getPortNames');
        
        $scope.refresh = function() {
            mrl.sendTo($scope.service.name, 'refresh');
        }
        
        $scope.disconnect = function() {
            mrl.sendTo($scope.service.name, 'disconnect')
        }
        
        $scope.connect = function(portName, baudrate, databits, stopbits, parity) {
            mrl.sendTo($scope.service.name, 'connect', portName, baudrate, databits, stopbits, parity);
        }
        
        $scope.writeString = function(txData) {
            mrl.sendTo($scope.service.name, 'writeString', txData);
        }
        
        $scope.connect = function(portName, baudrate, databits, stopbits, parity) {
            mrl.sendTo($scope.service.name, 'connect', portName, baudrate, databits, stopbits, parity);
        }

        //after you're done with setting up your service-panel, call this method
        $scope.gui.initDone();
    }]);
