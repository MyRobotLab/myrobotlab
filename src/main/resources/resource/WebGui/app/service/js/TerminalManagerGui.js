angular.module("mrlapp.service.TerminalManagerGui", []).controller("TerminalManagerGuiCtrl", [
  "$scope",
  "mrl",
  function ($scope, mrl) {
    console.info("TerminalManagerGuiCtrl")
    var _self = this
    var msg = this.msg

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function (service) {
      $scope.service = service
    }

    // init scope variables
    $scope.onTime = null
    $scope.onEpoch = null

    this.onMsg = function (inMsg) {
      let data = inMsg.data[0]
      switch (inMsg.method) {
        case "onState":
          _self.updateState(data)
          $scope.$apply()
          break
        case "onTime":
          const date = new Date(data)
          $scope.onTime = date.toLocaleString()
          $scope.$apply()
          break
        case "onEpoch":
          $scope.onEpoch = data
          $scope.$apply()
          break
        default:
          console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
          break
      }
    }

    // Assuming `service` is your service managing terminals
    $scope.startTerminal = function (key) {
      msg.send("startTerminal", key)
    }

    $scope.terminateTerminal = function (key) {
      msg.send("terminateTerminal", key)
    }

    $scope.saveTerminal = function (key) {
      msg.send("saveTerminal", key)
    }

    $scope.deleteTerminal = function (key) {
      msg.send("deleteTerminal", key)
    }

    msg.subscribe("publishEpoch")
    msg.subscribe(this)
  },
])
