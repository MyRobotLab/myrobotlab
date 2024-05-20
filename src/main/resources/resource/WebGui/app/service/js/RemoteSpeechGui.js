angular.module("mrlapp.service.RemoteSpeechGui", []).controller("RemoteSpeechGuiCtrl", [
  "$scope",
  "mrl",
  function ($scope, mrl) {
    console.info("RemoteSpeechGuiCtrl");
    var _self = this;
    var msg = this.msg;

    $scope.isEditing = false;

    this.updateState = function (service) {
      $scope.service = service;
    };

    this.onMsg = function (inMsg) {
      let data = inMsg.data[0];
      switch (inMsg.method) {
        case "onState":
          _self.updateState(inMsg.data[0]);
          $scope.$apply();
          break;
        case "onStatus":
          console.info("state", data);
          break;
        default:
          console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
          break;
      }
    };

    $scope.setType = function () {
      msg.send("setSpeechType", $scope.speechType);
    };

    $scope.speak = function (text) {
      msg.send("speak", text);
    };

    $scope.setVoice = function () {
      console.log($scope.service.voice.name);
      msg.send("setVoice", $scope.service.voice.name);
    };

    $scope.getUrl = function (service) {
      return service.config.endpoint?.url;
    };

    $scope.getVerb = function (service) {
      return service.config.endpoint?.verb;
    };

    $scope.getTemplate = function (service) {
      return service.config.endpoint?.template;
    };

    $scope.getAuthToken = function (service) {
      return service.config.endpoint?.authToken ? "********" : "";
    };

    $scope.toggleEdit = function () {
      $scope.isEditing = !$scope.isEditing;
      if ($scope.isEditing) {
        // Initialize form fields with current values
        $scope.editableUrl = $scope.getUrl($scope.service);
        $scope.editableVerb = $scope.getVerb($scope.service);
        $scope.editableTemplate = $scope.getTemplate($scope.service);
        $scope.editableAuthToken = $scope.getAuthToken($scope.service);
        $scope.editableSpeechType = $scope.speechType;
      }
    };

    $scope.handleSave = function () {
      // Add logic to handle saving the updated values
      console.log("Save button clicked");
      console.log("Updated URL:", $scope.editableUrl);
      console.log("Updated Verb:", $scope.editableVerb);
      console.log("Updated Template:", $scope.editableTemplate);
      console.log("Updated Auth Token:", $scope.editableAuthToken);
      console.log("Updated Speech Type:", $scope.editableSpeechType);

      // Update the service config with new values
      $scope.service.config.endpoint.url = $scope.editableUrl;
      $scope.service.config.endpoint.verb = $scope.editableVerb;
      $scope.service.config.endpoint.template = $scope.editableTemplate;
      $scope.service.config.endpoint.authToken = $scope.editableAuthToken;
      //$scope.service.config.endpoint.class = "org.myrobotlab.service.config.RemoteSpeechConfig$Endpoint"

      // Update the speechType and send the updated type to the backend
      $scope.speechType = $scope.editableSpeechType;
        msg.send("addSpeechType", $scope.speechType, $scope.service.config.endpoint);
     $scope.setType();      

      // Toggle back to view mode
      $scope.isEditing = false;
    };

    $scope.cancelEdit = function () {
      // Revert any changes and toggle back to view mode
      $scope.isEditing = false;
    };

    $scope.checkEnter = function (event) {
      if (event.which === 13) {
        // Enter key is keycode 13
        $scope.speak($scope.text);
        event.preventDefault(); // Prevent the default newline action
      }
    };

    msg.subscribe(this);
  },
]);
