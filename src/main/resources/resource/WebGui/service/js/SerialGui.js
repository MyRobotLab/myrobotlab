angular.module('mrlapp.service.SerialGui', [])
.controller('SerialGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('SerialGuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    
    $scope.monitorModel = false;

    // TODO - make global - part of ServiceGui
    // because any service can have queue speed
    $scope.stats = {
        "total":0,
        "interval":0,
        "ts":0,
        "lastTS":0,
        "delta":0,
        "lineSpeed":0
    };
    
    
    this.updateState = function(service) {
        $scope.service = service;
        $scope.isConnected = ($scope.service.portName != null );
        $scope.isConnectedImage = ($scope.service.portName != null ) ? "connected" : "disconnected";
        $scope.connectText = ($scope.service.portName == null ) ? "connect" : "disconnect";
        if ($scope.isConnected) {
            $scope.portName = $scope.service.portName;
        } else {
            $scope.portName = $scope.service.lastPortName;
        }
    }
    
    // initialization
    $scope.rx = "";
    $scope.rxCount = 0;
    $scope.tx = "";
    $scope.txCount = 0;
    $scope.txData = "";
    $scope.possiblePorts = [];
    $scope.possibleBaud = ['600', '1200', '2400', '4800', '9600', '19200', '38400', '57600', '115200'];
    $scope.portName = "";
    
    
    $scope.dynamicPopover = {
        content: 'Hello, World!',
        templateUrl: 'myPopoverTemplate2.html',
        title: 'Title'
    };
    
    // initial update
    $scope.service = mrl.getService($scope.service.name);
    _self.updateState($scope.service);
    
    //you HAVE TO define this method &
    //it is the ONLY exception of writing into .panel
    //-> you will receive all messages routed to your service here
    // FIXME - why this function on the scope? why is it on gui ? - i believe it should be on this.onMsg
    this.onMsg = function(msg) {
    	var maxLength = 1000;
        //$log.info('CALLBACK - ' + msg.method);
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
        case 'onStats':
            // backend update 
            //_self.updateState(msg.data[0]);
            $scope.stats = msg.data[0];
            $scope.$apply();
            break;
        case 'onRX':
            $scope.rx += ' ' + msg.data[0];
            ++$scope.rxCount;
            if ($scope.rx.length > maxLength) {
                $scope.rx = $scope.rx.substring($scope.rx.length - maxLength);
            }
            $scope.$apply();
            break;
        case 'onTX':
            ++$scope.txCount;
            $scope.tx += msg.data[0] + ' ';
            if ($scope.tx.length > maxLength) {
                $scope.tx = $scope.tx.substring($scope.tx.length - maxLength);
            }
            $scope.$apply();
            break;
        default:
            $log.error("ERROR - unhandled method " + msg.method);
            break;
        }
    }
    ;
    
    mrl.subscribe($scope.service.name, 'publishState');
    mrl.subscribe($scope.service.name, 'getPortNames');
    mrl.subscribe($scope.service.name, 'publishStats');
    
    mrl.sendTo($scope.service.name, 'broadcastState');
    mrl.sendTo($scope.service.name, 'getPortNames');
    
    $scope.monitor = function() {
        // mrl.sendTo($scope.service.name, 'refresh');
        $log.info('monitor', $scope.monitorModel);
        if ($scope.monitorModel) {
            mrl.subscribe($scope.service.name, 'publishTX');
            mrl.subscribe($scope.service.name, 'publishRX');
        } else {
            mrl.unsubscribe($scope.service.name, 'publishTX');
            mrl.unsubscribe($scope.service.name, 'publishRX');
        }
    }
    
    $scope.refresh = function() {
        mrl.sendTo($scope.service.name, 'getPortNames');
    }
    
    $scope.disconnect = function() {
        mrl.sendTo($scope.service.name, 'disconnect')
    }
    ;
    
    $scope.writeString = function(txData) {
        mrl.sendTo($scope.service.name, 'writeString', txData);
    }
    ;
    
    $scope.connect = function(portName, baudrate, databits, stopbits, parity) {
        mrl.sendTo($scope.service.name, 'connect', portName, baudrate, databits, stopbits, parity);
    }
    ;
    
    //after you're done with setting up your service-panel, call this method
    //    $scope.panel.initDone();

    msg.subscribe(this);
}
]);
