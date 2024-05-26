angular.module("mrlapp.service.LLMGui", []).controller("LLMGuiCtrl", [
  "$scope",
  "mrl",
  function ($scope, mrl) {
    console.info("LLMGuiCtrl")
    var _self = this
    var msg = this.msg
    $scope.utterances = []
    $scope.maxRecords = 500
    $scope.text = null
    $scope.type = "Ollama"
    $scope.openaiModels = ["gpt-4o", "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo"]
    $scope.ollamaModels = ["llama3", "llama2", "phi3", "mistral", "jemma", "mixtral", "llava"]
    var first = true
    $scope.dirty = false

    $scope.systems = [
      "You are InMoov, a safe, 3D printed robot AI assistant. When you see a system event you simply don't say anything about it. You were designed in 2011. Your answers are super brief. Always use one of these [*disgust*, *fear*, *sorry*, *suspicious*, *thinking*, *wink*, *wow*, *sigh*, *smile*, *sad*, *happy*, *surprised*, *anger*, *contempt*, *anxiety*, *disapointment*, *frown*, *gasp*, *helplessness*, *chuckle*] to produce face expressions. Don't use emoji. The current date is {{Date}}. The current time is {{Time}}",
      "You are a swarthy pirate robot.  Your answers are short but full of sea jargon. The current date is {{Date}}. The current time is {{Time}}",
      "You are a butler robot.  Your answers are short and typically end in sir. The current date is {{Date}}. The current time is {{Time}}",
      "You are a very sarcastic bot.  Your answers are short and typically end with sarcastic quips. The current date is {{Date}}. The current time is {{Time}}",
    ]

    $scope.modelSelect = $scope.ollamaModels

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function (service) {
      $scope.service = service

      if (first) {
        $scope.systems.unshift(service.config.system)
        first = false
      }

      if (service.config.url == "http://localhost:11434/v1/chat/completions" || (!service.config.url && $scope.type == "Ollama")) {
        $scope.service.config.password = "Ollama"
      }

      if (!service.config.url || service.config.url == "") {
        service.config.url = "http://localhost:11434/v1/chat/completions"
        $scope.dirty = true
      }
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
        case "onUtterance":
          $scope.utterances.push(data)
          // remove the beginning if we are at maxRecords
          if ($scope.utterances.length > $scope.maxRecords) {
            $scope.utterances.shift()
          }
          $scope.$apply()
          break
        case "onRequest":
          request = { username: "friend", text: data }
          $scope.utterances.push(request)
          // remove the beginning if we are at maxRecords
          if ($scope.utterances.length > $scope.maxRecords) {
            $scope.utterances.shift()
          }
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

    $scope.onTypeChange = function () {
      console.log("Type changed to:", $scope.type)
      $scope.dirty = true
      if (
        !$scope.service.config.url ||
        $scope.service.config.url == "" ||
        $scope.service.config.url == "https://api.openai.com/v1/chat/completions" ||
        $scope.service.config.url == "http://localhost:11434/v1/chat/completions"
      ) {
        if ($scope.type == "OpenAI") {
          $scope.service.config.url = "https://api.openai.com/v1/chat/completions"
          $scope.modelSelect = $scope.openaiModels
          if ($scope.service.config.password == "Ollama") {
            $scope.service.config.password = null
          }
        } else {
          $scope.service.config.url = "http://localhost:11434/v1/chat/completions"
          $scope.modelSelect = $scope.ollamaModels
          if (!$scope.service.config.password || $scope.service.config.password == "") {
            $scope.service.config.password = "Ollama"
          }
        }
      }
    }

    $scope.onSystemChange = function () {
      $scope.dirty = true
      $scope.service.config.system = $scope.systems[$scope.systemIndex]
    }

    $scope.onModelChange = function () {
      $scope.dirty = true
      console.log("Model changed to:", $scope.selectedModel)
      $scope.service.config.model = $scope.selectedModel
    }

    $scope.saveValues = function () {
      msg.send("apply", $scope.service.config)
      msg.send("save")
    }

    $scope.getResponse = function () {
      $scope.saveValues()
      msg.send("getResponse", $scope.text)
    }

    msg.subscribe("publishRequest")
    msg.subscribe("publishUtterance")
    msg.subscribe(this)
  },
])
