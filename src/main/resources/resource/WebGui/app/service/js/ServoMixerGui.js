angular.module("mrlapp.service.ServoMixerGui", []).controller("ServoMixerGuiCtrl", [
  "$scope",
  "mrl",
  function ($scope, mrl) {
    console.info("ServoMixerGuiCtrl")
    var _self = this
    var msg = this.msg
    var globalPoseIndex = 0

    $scope.minView = true
    $scope.sleep = 3 // initial
    $scope.showGestureSave = false
    $scope.speakBlocking = true
    $scope.mouth = "mouth"

    $scope.searchServo = {
      displayName: null,
    }

    $scope.servos = []
    $scope.sliders = []
    // list of current pose files
    $scope.poseFiles = []
    $scope.gestureFiles = []

    $scope.state = {
      // gestureIndex is a string representation from $index :( dumb
      gestureIndex: "0",
      selectedPose: null,
      selectedGestureFile: null,
      selectedGesture: null,
      playingPose: null,
      currentGesture: {
        parts: [],
      },
    }

    // unique id for new poses added to gesture
    let id = 0

    // FIXME - this should be done in a base class or in framework
    $scope.mrl = mrl

    // sublist object of servo panels - changes based onRegistered and onReleased events
    $scope.subPanels = {}

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function (service) {
      // do the update
      $scope.service = service
    }

    $scope.toggle = function (servo) {
      $scope.sliders[servo].tracking = !$scope.sliders[servo].tracking
    }

    _self.onSliderChange = function (servoName) {
      if (!$scope.sliders[servoName].tracking) {
        msg.sendTo(servoName, "moveTo", $scope.sliders[servoName].value)
      }
    }

    $scope.setSearchServo = function (text) {
      $scope.searchServo.displayName = text
    }

    this.updateState($scope.service)

    this.onMsg = function (inMsg) {
      var data = inMsg.data[0]
      switch (inMsg.method) {
        case "onStatus":
          console.log(inMsg)
          break
        case "onState":
          _self.updateState(data)
          $scope.$apply()
          break
        case "onSearch":
          $scope.searchServo.displayName = data
          $scope.searchServos(data)
          // sets pose name from selected
          $scope.state.selectedPose = data + "_" + globalPoseIndex++
          $scope.$apply()
          break
        case "onPlayingAction":
          // FIXME rename
          if (data.type != "Delay") {
            $scope.state.playingPose = data
          } else {
            $scope.state.playingPose.value = data.value / 1000
          }
          $scope.$apply()
          break
        case "onPlayingActionIndex":
          // FIXME rename
          $scope.state.gestureIndex = data + ""
          $scope.state.playingPoseIndex = data
          $scope.$apply()
          break
        case "onStopPose":
          // $scope.state.playingPose = ' '
          $scope.$apply()
          break
        case "onServoEvent":
          $scope.sliders[data.name].value = data.pos
          $scope.$apply()
          break
        case "onPoseFiles":
          $scope.poseFiles = data
          $scope.$apply()
          if (data && data.length > 0) {
            $scope.state.selectedPose = data[data.length - 1]
          }
          break
        case "onGesture":
          $scope.state.currentGesture = data
          $scope.$apply()
          break
        case "onGestureFiles":
          $scope.gestureFiles = data
          if (!$scope.state.selectedGestureFile && $scope.gestureFiles && $scope.gestureFiles.length > 0) {
            $scope.state.selectedGestureFile = $scope.gestureFiles[0]
            msg.send("getGesture", $scope.state.selectedGestureFile)
          }
          $scope.$apply()
          break
        case "onListAllServos":
          // servos sliders are either in "tracking" or "control" state
          // "tracking" they are moving from callback position info published by servos
          // "control" they are sending control messages to the servos
          $scope.servos = data
          for (var servo of $scope.servos) {
            // dynamically build sliders
            $scope.sliders[servo.name] = {
              value: 0,
              tracking: false,
              options: {
                id: servo.name,
                floor: 0,
                ceil: 180,
                onStart: function (id) {
                  console.info("ServoMixer.onStart")
                },
                onChange: function (id) {
                  _self.onSliderChange(id)
                },
                /*
                        onChange: function() {
                            if (!this.tracking) {
                                // if not tracking then control
                                msg.sendTo(servo, 'moveToX', sliders[servo].value)
                            }
                        },*/
                onEnd: function (id) {},
              },
            }
            // dynamically add callback subscriptions
            // these are "intermediate" subscriptions in that they
            // don't send a subscribe down to service .. yet
            // that must already be in place (and is in the case of Servo.publishServoEvent)
            // FIXME .. servo.getName() == servo.getFullName() :( - needs to be done in framework
            msg.subscribeTo(_self, servo.name + "@" + servo.id, "publishServoEvent")
          }
          $scope.$apply()
          break
        default:
          console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
          break
      }
    }

    $scope.addMoveToAction = function () {
      let moves = {}
      let hasSelected = false
      if ($scope.servos) {
        $scope.servos.forEach((servo) => {
          if (servo.selected) {
            moves[mrl.getShortName(servo.name)] = {
              position: servo.currentInputPos,
              speed: servo.speed,
            }
            hasSelected = true
          }
        })
      }

      if (hasSelected) {
        msg.send("addMoveToAction", moves, parseInt($scope.state.gestureIndex) + 1)
        msg.send("getGesture")
        $scope.state.gestureIndex = parseInt($scope.state.gestureIndex) + 1 + ""
      } else {
        mrl.send("error", "no selected servos")
      }
    }

    $scope.removeActionFromGesture = function () {
      msg.send("removeActionFromGesture", parseInt($scope.state.gestureIndex))
      msg.send("getGesture")
    }

    $scope.moveActionUp = function () {
      msg.send("moveActionUp", parseInt($scope.state.gestureIndex))
      // hilariously dumb need to subtract it and then convert it to a string
      $scope.state.gestureIndex = parseInt($scope.state.gestureIndex) - 1 + ""
    }

    $scope.moveActionDown = function () {
      msg.send("moveActionDown", parseInt($scope.state.gestureIndex))
      // hilariously dumb need to subtract it and then convert it to a string
      $scope.state.gestureIndex = parseInt($scope.state.gestureIndex) + 1 + ""
    }

    $scope.searchServos = function (searchText) {
      var result = {}
      angular.forEach($scope.subPanels, function (value, key) {
        if (!searchText || mrl.getShortName(key).indexOf(searchText) != -1) {
          result[key] = value
        }
      })
      return result
    }

    $scope.savePose = function (pose) {
      msg.send("savePose", pose)
    }

    // this method initializes subPanels when a new service becomes available
    this.onRegistered = function (panel) {
      if (panel.simpleName == "Servo") {
        $scope.subPanels[panel.name] = panel
      }
    }

    // this method removes subPanels references from released service
    this.onReleased = function (panelName) {
      delete $scope.subPanels[panelName]
      console.info("here")
    }

    $scope.step = function () {
      let index = parseInt($scope.state.gestureIndex)
      let part = $scope.state.currentGesture.parts[index]
      if (part.type === "Pose") {
        msg.send("moveToPose", part.name)
      }
      index++
      $scope.state.gestureIndex = index + ""
    }

    // initialize all services which have panel references in Intro
    let servicePanelList = mrl.getPanelList()
    for (let index = 0; index < servicePanelList.length; ++index) {
      this.onRegistered(servicePanelList[index])
    }

    $scope.saveGesture = function (gestureName) {
      msg.send("saveGesture", gestureName)
    }

    $scope.addSleep = function (seconds) {
      let value = parseFloat(seconds)

      if (Number.isNaN(value)) {
        console.error(seconds, "is not a valid number for sleep")
        return
      }

      msg.send("addSleepAction", seconds)
      msg.send("getGesture")
    }

    $scope.playGesture = function (gesture) {
      if (gesture) {
        msg.send("playGesture", gesture)
      } else {
        console.warn("gesture empty")
      }
    }

    $scope.removeGesture = function (gesture) {
      if (gesture) {
        msg.send("removeGesture", gesture)
      } else {
        console.warn("removeGesture empty")
      }
    }

    $scope.displayAction = function (action) {
      if (action.type == "sleep") {
        return `sleep ${action.value}`
      } else if (action.type == "speak") {
        return `speak ${action.value.text}`
      } else if (action.type == "moveTo") {
        let ret = "move"
        Object.keys(action.value).forEach((key) => {
          ret += ` ${key} ${action.value[key].position}`
          if (action.value[key].speed) {
            ret += ` ${action.value[key].speed}`
          }
        })
        return ret
      } else {
        return action.value
      }
    }

    $scope.speak = function () {
      speechPart = {
        mouth: $scope.mouth,
        blocking: $scope.speakBlocking,
        text: $scope.text,
      }
      msg.send("addSpeakAction", speechPart, parseInt($scope.state.gestureIndex) + 1)
      msg.send("getGesture")
    }

    $scope.toggleServoSpeedBar = function (service) {
      service.speedBar = !service.speedBar
    }

    msg.subscribe("getGesture")
    msg.subscribe("getGestureFiles")
    msg.subscribe("listAllServos")
    msg.subscribe("search")

    msg.send("listAllServos")
    msg.send("getGestureFiles")

    msg.subscribe("publishPlayingAction")
    msg.subscribe("publishPlayingActionIndex")
    msg.subscribe("publishStopPose")

    mrl.subscribeToRegistered(this.onRegistered)
    mrl.subscribeToReleased(this.onReleased)

    msg.subscribe(this)
  },
])
