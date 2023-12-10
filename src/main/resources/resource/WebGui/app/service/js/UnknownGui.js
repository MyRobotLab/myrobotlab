angular.module("mrlapp.service.UnknownGui", []).controller("UnknownGuiCtrl", [
  "$scope",
  "mrl",
  function ($scope, mrl) {
    console.info("UnknownGuiCtrl")
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
        default:
          console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
          break
      }
    }

    msg.subscribe(this)
  },
])
