angular.module("mrlapp.service.EmailGui", []).controller("EmailGuiCtrl", [
  "$scope",
  "mrl",
  function ($scope, mrl) {
    console.info("EmailGuiCtrl")
    var _self = this
    var msg = this.msg

    // Toggle edit mode for a specific property
    $scope.toggleEdit = function (key) {
      $scope.editMode[key] = !$scope.editMode[key]
    }

    // Remove a property from the service
    $scope.removeProp = function (key) {
      msg.send("removeProperty", key)
      msg.send("broadcastState")
    }

    // New property template
    $scope.newProp = { name: "", value: "" }

    // Add a new property to the service
    $scope.addProp = function () {
      if ($scope.newProp.name && $scope.newProp.value) {
        msg.send("addProperty", $scope.newProp.name, $scope.newProp.value)
        msg.send("broadcastState")
        $scope.newProp.name = ""
        $scope.newProp.value = ""
      }
    }

    // Email data
    $scope.emailMessage = ""
    $scope.attachment = null

    // Handle file selection
    $scope.handleFileChange = function (event) {
      $scope.$apply(() => {
        $scope.attachment = event.target.files[0]
      })
    }

    // Send email function (placeholder)
    $scope.sendEmail = function () {
      if (!$scope.service.config.to.trim()) {
        alert("Please enter a recipient email.")
        return
      }

      if (!$scope.emailMessage.trim()) {
        alert("Please enter an email message.")
        return
      }

      console.log("Sending email...")
      console.log("To:", $scope.service.config.to)
      console.log("Message:", $scope.emailMessage)
      console.log("SMTP Config:", $scope.service.props)
      if ($scope.attachment) {
        console.log("Attachment:", $scope.attachment.name)
      }
      // msg.send("broadcastState")
      msg.send("sendEmail", $scope.service.config.to, $scope.emailSubject, $scope.emailMessage, $scope.emailFormat, $scope.attachment)
    }

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

    // msg.subscribe("publishTime")
    // msg.subscribe("publishEpoch")
    msg.subscribe(this)
  },
])
