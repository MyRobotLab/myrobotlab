angular.module("mrlapp.service.VLMGui", []).controller("VLMGuiCtrl", [
  "$scope",
  "mrl",
  function ($scope, mrl) {
    console.info("VLMGuiCtrl")
    var _self = this
    var msg = this.msg

    $scope.utterances = []
    $scope.maxRecords = 500

    // the question / instruction sent with the image
    $scope.prompt = "What is in this image?"
    // a uri or local path to an image
    $scope.imageUri = ""
    // selected opencv (or other ImagePublisher) service to grab a frame from
    $scope.cvName = null
    $scope.cvServices = []

    $scope.dirty = false

    // common open source vision models that can be run offline through Ollama
    $scope.visionModels = ["llava", "llava:13b", "llava-llama3", "bakllava", "llama3.2-vision", "moondream", "minicpm-v", "qwen2.5vl"]

    // discover services that can publish images (OpenCV, etc.)
    $scope.loadCvServices = function () {
      $scope.cvServices = []
      let services = mrl.getServicesFromInterface("org.myrobotlab.service.interfaces.ImagePublisher")
      for (let i = 0; i < services.length; i++) {
        $scope.cvServices.push(services[i].name)
      }
      if (!$scope.cvName && $scope.cvServices.length > 0) {
        $scope.cvName = $scope.cvServices[0]
      }
    }

    this.updateState = function (service) {
      $scope.service = service
    }

    this.onMsg = function (inMsg) {
      let data = inMsg.data[0]
      switch (inMsg.method) {
        case "onState":
          _self.updateState(data)
          $scope.loadCvServices()
          $scope.$apply()
          break
        case "onUtterance":
          $scope.utterances.push(data)
          if ($scope.utterances.length > $scope.maxRecords) {
            $scope.utterances.shift()
          }
          $scope.$apply()
          break
        case "onRequest":
          $scope.utterances.push({ username: "you", text: data })
          if ($scope.utterances.length > $scope.maxRecords) {
            $scope.utterances.shift()
          }
          $scope.$apply()
          break
        case "onImageRequest":
          $scope.utterances.push({ username: "you", text: data.prompt, img: data.img })
          if ($scope.utterances.length > $scope.maxRecords) {
            $scope.utterances.shift()
          }
          $scope.$apply()
          break
        default:
          console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
          break
      }
    }

    $scope.onModelChange = function () {
      $scope.dirty = true
      $scope.service.config.model = $scope.selectedModel
    }

    $scope.saveValues = function () {
      msg.send("apply", $scope.service.config)
      msg.send("save")
      $scope.dirty = false
    }

    // analyze an image referenced by a uri or local path - the service echoes
    // back a publishImageRequest which renders the request bubble
    $scope.analyzeUri = function () {
      if (!$scope.imageUri) {
        return
      }
      msg.send("getResponse", $scope.prompt, $scope.imageUri)
    }

    // grab a frame from the selected opencv service and analyze it
    $scope.grabFrame = function () {
      if (!$scope.cvName) {
        return
      }
      msg.send("getResponseFromCamera", $scope.cvName, $scope.prompt)
    }

    msg.subscribe("publishRequest")
    msg.subscribe("publishImageRequest")
    msg.subscribe("publishUtterance")
    msg.subscribe(this)
  },
])
