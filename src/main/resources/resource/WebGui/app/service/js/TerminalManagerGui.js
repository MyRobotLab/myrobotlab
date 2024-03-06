angular.module("mrlapp.service.TerminalManagerGui", []).controller("TerminalManagerGuiCtrl", ["$scope", "mrl", function($scope, mrl) {
    console.info("TerminalManagerGuiCtrl")
    var _self = this
    var msg = this.msg

    $scope.processCommand = function(key, input) {
        msg.send("processCommand", key, input)
        $scope.service.inputValue = ""
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case "onState":
            $scope.service = data
            $scope.$apply()
            break
        case "onLog":
            $scope.service.terminals[data.terminal].output = $scope.service.terminals[data.terminal].output + data.msg
            let length = $scope.service.terminals[data.terminal].output.length
            if (length > 1024) {
                let overLength = length - 1024;
                $scope.service.terminals[data.terminal].output = $scope.service.terminals[data.terminal].output.substring(overLength);
            }
            $scope.$apply()
            break
        case "onStdOut":
            break
        case "onCmd":
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    // Assuming `service` is your service managing terminals
    $scope.startTerminal = function(key) {
        msg.send("startTerminal", key)
    }

    $scope.terminateTerminal = function(key) {
        msg.send("terminateTerminal", key)
    }

    $scope.saveTerminal = function(key) {
        msg.send("saveTerminal", key)
    }

    $scope.deleteTerminal = function(key) {
        msg.send("deleteTerminal", key)
    }

    msg.subscribe("publishLog")
    // msg.subscribe("publishStdOut")
    msg.subscribe("publishCmd")
    msg.subscribe(this)
}
, ])
