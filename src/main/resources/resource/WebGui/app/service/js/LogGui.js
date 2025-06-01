angular.module("mrlapp.service.LogGui", []).controller("LogGuiCtrl", [
  "$scope",
  "mrl",
  function ($scope, mrl) {
    console.info("LogGuiCtrl")
    var _self = this
    var msg = this.msg

    var name = $scope.name
    //    var msg = mrl.createMsgInterface(name, $scope)
    // TODO singleton -  clear log / log level / appenderes / format

    // init scope variables
    $scope.logButton = ""
    $scope.filterLevelValue = 0
    $scope.rowCount = 0
    $scope.loggers = {
      any: "any",
    }
    $scope.threads = {
      any: "any",
    }
    $scope.threadFilter = "any"
    $scope.loggerFilter = "any"
    $scope.reverse = false
    $scope.maxRecords = 1000
    $scope.pauseText = "pause"

    $scope.logLevelValue = {
      DEBUG: 0,
      INFO: 1,
      WARN: 2,
      ERROR: 3,
    }

    $scope.valueToLevel = function (value) {
      switch (value) {
        case 0:
          return "DEBUG"
        case 1:
          return "INFO"
        case 2:
          return "WARN"
        case 3:
          return "ERROR"
        default:
          return "UNKNOWN"
          break
      }
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function (service) {
      $scope.service = service
      // set logging state
      if (service.isLogging == true) {
        $scope.logButton = "stop"
      } else {
        $scope.logButton = "start"
      }
    }

    this.onMsg = function (msg) {
      switch (msg.method) {
        case "onState":
          _self.updateState(msg.data[0])
          $scope.$apply()
          break
        case "onLogLevel":
          $scope.service.logLevel = msg.data[0]
          break

        case "onLog":
          // let binaryString = $scope.base64ToArrayBuffer(msg.data[0]);
          var textFileAsBlob = new Blob([msg.data[0]])

          var downloadLink = document.createElement("a")
          downloadLink.download = "myrobotlab.log"
          downloadLink.innerHTML = "Download File"
          if (window.webkitURL != null) {
            // Chrome allows the link to be clicked
            // without actually adding it to the DOM.
            downloadLink.href = window.webkitURL.createObjectURL(textFileAsBlob)
          } else {
            // Firefox requires the link to be added to the DOM
            // before it can be clicked.
            downloadLink.href = window.URL.createObjectURL(textFileAsBlob)
            downloadLink.onclick = destroyClickedElement
            downloadLink.style.display = "none"
            document.body.appendChild(downloadLink)
          }

          downloadLink.click()
          break
        case "onLogEvents":
          let events = msg.data[0]
          var length = $scope.service.logs.length

          if ($scope.pauseText == "pause") {
            events.forEach(function (e) {
              $scope.service.logs.push(e)
              $scope.rowCount++
              $scope.threads[e.threadName] = e.threadName
              $scope.loggers[e.className.substring(e.className.lastIndexOf(".") + 1)] = e.className

              // remove the beginning if we are at maxRecords
              if ($scope.service.logs.length > $scope.maxRecords) {
                $scope.service.logs.shift()
              }
            })

            // $scope.service.logs.concat(msg.data[0])
            $scope.$apply()
          }
          break
        default:
          console.error("ERROR - unhandled method " + $scope.name + "." + msg.method)
          break
      }
    }

    $scope.clear = function () {
      $scope.service.logs = []
      msg.send("clear")
      $scope.$apply()
    }

    $scope.getLogLevelValue = function (level) {
      return $scope.logLevelValue[level]
    }

    $scope.setFilterLevelValue = function (level) {
      $scope.filterLevelValue = level
      $scope.$apply()
    }

    $scope.setLoggerFilter = function (logger) {
      $scope.loggerFilter = logger
      // shortname to fullname
      $scope.$apply()
    }

    $scope.setThreadFilter = function (t) {
      $scope.threadFilter = t
      $scope.$apply()
    }

    $scope.pause = function () {
      $scope.pauseText = $scope.pauseText == "pause" ? "unpause" : "pause"
    }

    $scope.showRecord = function (e) {
      return (
        $scope.filterLevelValue <= $scope.getLogLevelValue(e.level) &&
        ($scope.loggerFilter == "any" || e.className == $scope.loggerFilter) &&
        ($scope.threadFilter == "any" || e.threadName == $scope.threadFilter)
      )
    }

    $scope.toggle = function (label, interval) {
      if (label == "Start") {
        mrl.sendTo($scope.service.name, "startLog")
      } else {
        mrl.sendTo($scope.service.name, "stopLog")
      }
    }

    msg.subscribe("getLog")
    msg.subscribe("getLogLevel")
    msg.subscribe("publishLogEvents")
    msg.subscribe(this)
  },
])
