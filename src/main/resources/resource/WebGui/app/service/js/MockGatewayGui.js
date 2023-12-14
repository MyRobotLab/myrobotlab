angular.module("mrlapp.service.MockGatewayGui", []).controller("MockGatewayGuiCtrl", [
  "$scope",
  "mrl",
  function ($scope, mrl) {
    console.info("MockGatewayGuiCtrl")
    var _self = this
    var msg = this.msg

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function (service) {
      $scope.service = service
    }

    $scope.msgToRemote = function (msg) {
      if ($scope.service) {
        return msg.name.endsWith($scope.service.remoteId)
      } else {
        return false
      }
    }

    this.onMsg = function (inMsg) {
      let data = inMsg.data[0]
      switch (inMsg.method) {
        case "onState":
          _self.updateState(data)
          $scope.$apply()
          break
        case "onMessageEvent":
          $scope.service.msgs.push(data)
          $scope.$apply()
          break
        default:
          console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
          break
      }
    }

    msg.subscribe("publishMessageEvent")
    msg.subscribe(this)
  },
])
