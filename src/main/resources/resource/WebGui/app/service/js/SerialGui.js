angular.module('mrlapp.service.SerialGui', []).controller('SerialGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('SerialGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.monitorModel = false

    // TODO - make global - part of ServiceGui
    // because any service can have queue speed
    $scope.stats = {
        "total": 0,
        "interval": 0,
        "ts": 0,
        "lastTS": 0,
        "delta": 0,
        "lineSpeed": 0
    }

    this.updateState = function(service) {
        $scope.service = service
        $scope.isConnected = ($scope.service.portName != null)
        $scope.isConnectedImage = ($scope.service.portName != null) ? "connected" : "disconnected"
        $scope.connectText = ($scope.service.portName == null) ? "connect" : "disconnect"
        if ($scope.isConnected) {
            $scope.portName = $scope.service.portName
        } else {
            $scope.portName = $scope.service.lastPortName
        }
    }

    // initialization
    $scope.format = "hex"
    $scope.delimiter = "none"
    $scope.sendFormat = "ascii"
    $scope.rx = ""
    $scope.rxCount = 0
    $scope.tx = ""
    $scope.txCount = 0
    $scope.txData = ""
    $scope.possiblePorts = []
    $scope.possibleBaud = ['600', '1200', '2400', '4800', '9600', '19200', '38400', '57600', '115200']
    $scope.portName = ""

    $scope.dynamicPopover = {
        content: 'Hello, World!',
        templateUrl: 'myPopoverTemplate2.html',
        title: 'Title'
    }

    //you HAVE TO define this method &
    //it is the ONLY exception of writing into .panel
    //-> you will receive all messages routed to your service here
    // FIXME - why this function on the scope? why is it on gui ? - i believe it should be on this.onMsg
    this.onMsg = function(msg) {
        var maxLength = 1000
        //$log.info('CALLBACK - ' + msg.method)
        switch (msg.method) {
        case 'onPortNames':
            $scope.possiblePorts = msg.data[0]
            $scope.$apply()
            break
        case 'onRefresh':
            $scope.possiblePorts = msg.data[0]
            $scope.$apply()
            break
        case 'onState':
            // backend update 
            _self.updateState(msg.data[0])
            $scope.$apply()
            break
        case 'onStats':
            // backend update 
            //_self.updateState(msg.data[0])
            $scope.stats = msg.data[0]
            $scope.$apply()
            break
        case 'onStatus':
            // backend update 
            //_self.updateState(msg.data[0])
            $scope.status = msg.data[0]
            $scope.$apply()
            break
        case 'onRX':
            $scope.rx += _self.format(msg.data[0])
            ++$scope.rxCount
            if ($scope.rx.length > maxLength) {
                $scope.rx = $scope.rx.substring($scope.rx.length - maxLength)
            }
            $scope.$apply()
            break
        case 'onTX':
            ++$scope.txCount
            $scope.tx += _self.format(msg.data[0])
            if ($scope.tx.length > maxLength) {
                $scope.tx = $scope.tx.substring($scope.tx.length - maxLength)
            }
            $scope.$apply()
            break
        default:
            $log.error("ERROR - unhandled method " + msg.method)
            break
        }
    }

    this.format = function(value) {
        let newVal = null;
        if ($scope.format == 'hex') {
            // newVal = parseInt(value, 16)
            newVal = value.toString(16).toUpperCase()
        } else {
            newVal = value
        }

        if ($scope.delimiter == 'space') {
            newVal += ' '
        } else if ($scope.delimiter == 'newline') {
            newVal += '\n'
        }
        return newVal
    }

    $scope.clearConsoles = function() {
        $scope.rx = ""
        $scope.rxCount = 0
        $scope.tx = ""
        $scope.txCount = 0
    }

    $scope.refresh = function() {
        msg.send('getPortNames')
    }

    $scope.disconnect = function() {
        msg.send('disconnect')
    }

    $scope.writeString = function(txData) {
        if ($scope.sendFormat == 'hex') {
            let parts = txData.split(' ')
            for (part in parts) {
                let x = parseInt(parts[part], 16)
                msg.send('writeInt', x)
            }
        } else if ($scope.sendFormat == 'decimal') {
            let parts = txData.split(' ')
            for (part in parts) {
                let x = parseInt(parts[part], 10)
                msg.send('writeInt', x)
            }
        } else {
            msg.send('writeString', txData)
        }

    }

    $scope.connect = function(portName, baudrate, databits, stopbits, parity) {
        msg.send('connect', portName, baudrate, databits, stopbits, parity)
    }

    //after you're done with setting up your service-panel, call this method
    //    $scope.panel.initDone()

    // mrl.subscribe('publishState')
    msg.subscribe('getPortNames')
    msg.subscribe('publishStats')

    msg.send('broadcastState')
    msg.send('getPortNames')

    $scope.monitor = function() {

        $log.info('monitor', $scope.monitorModel)
        if ($scope.monitorModel) {
            msg.subscribe('publishTX')
            msg.subscribe('publishRX')
        } else {
            msg.unsubscribe('publishTX')
            msg.unsubscribe('publishRX')
        }
    }

    msg.subscribe(this)
}
])
