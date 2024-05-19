angular.module("mrlapp.service.RemoteSpeechGui", []).controller("RemoteSpeechGuiCtrl", [
  "$scope",
  "mrl",
  function ($scope, mrl) {
    console.info("RemoteSpeechGuiCtrl")
    var _self = this
    var msg = this.msg

    this.updateState = function (service) {
      $scope.service = service
    }

    this.onMsg = function (inMsg) {
      let data = inMsg.data[0]
      switch (inMsg.method) {
        case "onState":
          _self.updateState(inMsg.data[0])
          $scope.$apply()
          break
        case "onStatus":
          console.info("state", data)
          break
        default:
          console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
          break
      }
    }

    $scope.setType = function () {
      msg.send("setSpeechType", $scope.speechType)
    }

    $scope.speak = function (text) {
      msg.send("speak", text)
    }

    $scope.setVoice = function () {
      console.log($scope.service.voice.name)
      msg.send("setVoice", $scope.service.voice.name)
    }

    $scope.getUrl = function (service) {
      return service.config.endpoint?.url
    }

    $scope.getVerb = function (service) {
      return service.config.endpoint?.verb
    }

    $scope.getTemplate = function (service) {
      return service.config.endpoint?.template
    }

    $scope.getAuthToken = function (service) {
      return service.config.endpoint?.authToken
    }

    msg.subscribe(this)
  },
])
