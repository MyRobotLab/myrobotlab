angular.module('mrlapp.service.Hd44780Gui', []).controller('Hd44780GuiCtrl', ['$scope', 'mrl', function ($scope, mrl) {
    // $modal ????
    console.info('Hd44780GuiCtrl')
    // grab the self and message
    var _self = this
    var msg = this.msg

    // use $scope only when the variable
    // needs to interract with the display
    $scope.backLight = false
    $scope.screenContent = ''

    // start info status
    $scope.rows = []

    // following the template.
    this.updateState = function (service) {
        // use another scope var to transfer/merge selection
        // from user - service.currentSession is always read-only
        // all service data should never be written to, only read from
        $scope.screenContent = ''
        Object.keys(service.screenContent).forEach(function (key) {
            $scope.screenContent += service.screenContent[key].trim() + '\n'
            //console.log(service.screenContent[key])
        })
        $scope.backLight = service.backLight
        $scope.service = service
    }

    this.onMsg = function (inMsg) {
        console.info("Hd44780 Msg !")
        let data = inMsg.data[0]

        switch (inMsg.method) {
            case 'onStatus':
                break
            case 'onClear':
                $scope.screenContent = ''
                $scope.$apply()
                break
            case 'onState':
                _self.updateState(data)
                $scope.$apply()
                break
            default:
                console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
                break
        }
    }

    $scope.setBackLight = function (status) {
        msg.send("setBackLight", status)
    }

    $scope.display = function (textArea) {
        console.info('setBackLicght')
        msg.send("clear")
        var lines = textArea.split('\n')
        for (var i = 0; i < lines.length && i < 4; i++) {
            msg.send("display", lines[i], i)
            //console.info("Hd44780 Msg send !")
        }
    }


    _self.setControllerName = function () {
        // $scope.service.controllerName = controller
        // msg.send('attach', $scope.service.deviceAddress)
    }

    $scope.options = {
        interface: 'I2CControl',
        attach: _self.setControllerName,
        // callback: function...
        attachName: $scope.service.pcfName
    }

    $scope.attach = function () {
        msg.send("attach", $scope.options.attachName)
    }

    $scope.detach = function () {
        msg.send("detach")
    }

    $scope.clear = function () {
        msg.send("clear")
    }

    $scope.reset = function () {
        msg.send("reset")
    }

    msg.subscribe('clear')

    // subscribe to the response
    msg.subscribe(this)
}
])
