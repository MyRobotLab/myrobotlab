angular.module("mrlapp.service.RuntimeGui", []).controller("RuntimeGuiCtrl", [
  "$scope",
  "mrl",
  "statusSvc",
  "$timeout",
  "$uibModal",
  "modalService",
  function ($scope, mrl, statusSvc, $timeout, $uibModal, modalService) {
    console.info("RuntimeGuiCtrl")
    var _self = this
    var msg = this.msg

    var statusMaxSize = 2500

    // configName is static so it needs to be
    // kept in sync on a subobject
    $scope.selected = {
      configName: "default",
    }

    this.updateState = function (service) {
      $scope.service = service
      $scope.locale.selected = service.locale.language
      $scope.localeTag.selected = service.locale
    }

    $scope.locales = {}
    $scope.status = ""
    $scope.cmd = ""
    $scope.registry = {}
    $scope.connections = {}
    $scope.newName = null
    $scope.newType = ""
    $scope.heartbeatTs = null
    $scope.hosts = []
    // $scope.selectedOption = "current"

    $scope.languages = {
      en: {
        language: "en",
        displayLanguage: "English",
      },
    }

    $scope.locale = {
      selected: null,
    }

    $scope.localeTag = {
      selected: {
        tag: "en-US",
      },
    }

    $scope.category = {
      selected: null,
    }

    $scope.deleteConfig = function () {
      console.info("deleteConfig", $scope.selected.configName)
      msg.send("deleteConfig", $scope.selected.configName)
      $scope.selected.configName = null
    }

    $scope.categoryServiceTypes = null

    $scope.disabled = undefined
    $scope.person = {}

    var msgKeys = {}

    let categoryServiceTypes = null

    // $scope.categoryServiceTypes = $scope.service.serviceData.categoryTypes[$scope.category.selected].serviceTypes

    $scope.filterServices = function () {
      var result = {}
      // console.debug('$scope.category.selected is ' + $scope.category.selected)
      const entries = Object.entries($scope.service.serviceData.serviceTypes)

      if ($scope.category.selected != null && $scope.category.selected == "show all") {
        return $scope.service.serviceData.serviceTypes
      }

      for (const [fullTypeName, metaData] of entries) {
        // if (metaData.simpleName.toLowerCase().includes($scope.newType)) {

        if ($scope.category.selected != null) {
          categoryServiceTypes = $scope.service.serviceData.categoryTypes[$scope.category.selected].serviceTypes
        } else {
          categoryServiceTypes = null
        }

        if (/*metaData.simpleName.toLowerCase().includes($scope.newType) && */ categoryServiceTypes != null && categoryServiceTypes.includes(metaData.name)) {
          result[fullTypeName] = metaData
        }
      }
      return result
    }

    // FIXME - maintain contextPath !!!
    $scope.sendToCli = function (cmd) {
      console.log("sendToCli " + cmd)
      $scope.cmd = ""
      contextPath = null
      msg.send("sendToCli", "runtime@" + mrl.getId(), cmd)
    }

    $scope.setServiceType = function (serviceType) {
      $scope.newType = serviceType
    }

    $scope.setConfig = function () {
      console.info("setConfig")
      if ($scope.selectedConfig.length > 0) {
        msg.sendTo("runtime", "setConfig", $scope.selectedConfig[0])
        msg.sendTo("runtime", "getConfigName")
      }
    }

    $scope.start = function () {
      if ($scope.newName == null) {
        mrl.error("name of service is required")
        return
      }
      if ($scope.newType == null) {
        mrl.error("type of service is required")
        return
      }

      if (typeof $scope.newType == "object") {
        $scope.newType = $scope.newType.simpleName
      }
      msg.send("start", $scope.newName, $scope.newType)

      $scope.newName = null
      $scope.newType = null
    }

    this.onMsg = function (inMsg) {
      let data = null
      if (inMsg.data) {
        data = inMsg.data[0]
      }

      switch (inMsg.method) {
        case "onState":
          _self.updateState(data)
          $scope.$apply()
          break
        case "onPlan":
          $scope.plan = data
          $scope.$apply()
          break
        case "onLocalServices":
          $scope.registry = data
          //  $scope.$apply()
          break
        case "onLocale":
          $scope.locale.selected = data.language
          $scope.$apply()
          break
        case "onConfigList":
          if (data) {
            $scope.service.configList = data.sort()
            $scope.$apply()
          }
          break

        case "onStartYml":
          $scope.startYml = data
          $scope.$apply()
          break

        case "onSaveDefaults":
          if (data.length > 0) {
            $scope.defaultsSaved = "saved defaults to " + data
            msg.send("publishConfigList")
            $scope.$apply()
          } else {
            ;("service does not have defaults")
          }
          break

        case "onInterfaceToNames":
          $scope.interfaceToPossibleServices = data
          mrl.interfaceToPossibleServices = data
          break

        case "onServiceTypes":
          $scope.serviceTypes = data
          mrl.setPossibleServices($scope.serviceTypes)
          break

        case "onRegistered":
          console.log("onRegistered")
          break

        case "onConnections":
          $scope.connections = data
          $scope.$apply()
          break
        case "onHosts":
          $scope.hosts = data
          $scope.$apply()
          break
        case "onStatus":
          $scope.status = data.name + " " + data.level + " " + data.detail + "\n" + $scope.status
          if ($scope.status.length > 300) {
            $scope.status = $scope.status.substring(0, statusMaxSize)
          }
          break
        case "onCli":
          if (data != null) {
            $scope.status = JSON.stringify(data, null, 2) + "\n" + $scope.status
            if ($scope.status.length > 300) {
              $scope.status = $scope.status.substring(0, statusMaxSize)
            }
            $scope.$apply()
          } else {
            $scope.status += "null\n"
          }
          break
        case "onReleased":
          console.info("runtime - onRelease " + data)
          break
        case "onConfigName":
          console.info("runtime - onConfigName " + data)
          // is not part of service, because configName is static
          $scope.selected.configName = data
          $scope.$apply()
          break
        case "onHeartbeat":
          let heartbeat = data
          let hb = heartbeat.name + "@" + heartbeat.id + " sent onHeartbeat - "
          $scope.heartbeatTs = heartbeat.ts
          $scope.$apply()

          for (let i in heartbeat.serviceList) {
            let serviceName = heartbeat.serviceList[i].name + "@" + heartbeat.serviceList[i].id
            hb += serviceName + " "

            // FIXME - 'merge' ie remove missing services

            // FIXME - want to maintain "local" registry ???
            // currently maintaining JS process registry - should the RuntimeGui also maintain
            // its 'own' sub-registry ???
            if (!serviceName in mrl.getRegistry()) {
              //
              console.warn(serviceName + " not defined in registry - sending registration request")
            }
            // else already registered
          }

          console.info(hb)

          // CHECK REGISTRY
          // SYNC SERVICES
          // REQUEST REGISTRATIONS !!!!
          break
        default:
          console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
          break
      }
    }

    $scope.shutdown = function (type) {
      var modalInstance = $uibModal.open({
        //animation: true,
        // templateUrl: 'nav/shutdown.html',
        // template: '<div class="modal-header"> HELLO ! </div>',
        // controller: $scope.doShutdown,
        // controller: 'RuntimeGuiCtrl',
        scope: $scope,
        // controller: 'ModalController',

        animation: true,
        templateUrl: "nav/shutdown.html",
        controller: "shutdownCtrl2",

        resolve: {
          type: function () {
            return type
          },
        },
      })
      console.info("shutdown " + modalInstance)
    }

    $scope.setAllLocales = function (locale) {
      console.info(locale)
    }

    $scope.loadConfig = function () {
      console.info("loadConfig")
      if ($scope.selectedConfig.length) {
        for (let i = 0; i < $scope.selectedConfig.length; ++i) {
          // msg.sendTo('runtime', 'load', 'data/config/' + $scope.selectedConfig[i] + '/runtime.yml')
          msg.sendTo("runtime", "setConfig", $scope.selectedConfig[i])
          msg.sendTo("runtime", "load", "runtime")
        }
      }
    }

    $scope.unsetConfig = function () {
      console.info("unsetConfig")
      msg.sendTo("runtime", "unsetConfig")
    }

    $scope.startConfig = function () {
      console.info("startConfig")
      if ($scope.selectedConfig.length) {
        for (let i = 0; i < $scope.selectedConfig.length; ++i) {
          // msg.sendTo('runtime', 'load', 'data/config/' + $scope.selectedConfig[i] + '/runtime.yml')
          msg.sendTo("runtime", "startConfig", $scope.selectedConfig[i])
        }
      }
    }

    $scope.releaseConfig = function () {
      console.info("releaseConfig")
      if ($scope.selectedConfig && $scope.selectedConfig.length) {
        for (let i = 0; i < $scope.selectedConfig.length; ++i) {
          msg.sendTo("runtime", "releaseConfig", $scope.selectedConfig[i])
          // msg.sendTo('runtime', 'releaseConfig', 'runtime')
        }
      } else {
        msg.sendTo("runtime", "releaseConfig")
      }
    }

    $scope.savePlan = function () {
      console.info("saveConfig")

      let onOK = function () {
        msg.sendTo("runtime", "savePlan", $scope.selected.configName)
        // msg.sendTo('runtime', 'save')
      }

      let onCancel = function () {
        console.info("save config cancelled")
      }

      let ret = modalService.openOkCancel(
        "widget/modal-dialog.view.html",
        "Save Plan Configuration",
        "Save your current configuration in a directory named",
        onOK,
        onCancel,
        $scope
      )
      console.info("ret " + ret)
    }

    $scope.saveDefaults = function () {
      console.info("saveDefaults")
      msg.send("saveDefaults", $scope.newType.simpleName)
    }

    $scope.setAutoStart = function (b) {
      console.info("setAutoStart")
      msg.send("setAutoStart", b)
    }

    $scope.saveConfig = function () {
      $scope.service.includePeers = false
      $scope.service.selectedOption = "current"

      $scope.selected.configName = $scope.selected.configName
      var modalInstance = $uibModal.open({
        templateUrl: "saveConfig.html",
        scope: $scope,
        controller: function ($scope, $uibModalInstance) {
          $scope.ok = function () {
            $uibModalInstance.close()
          }

          $scope.cancel = function () {
            $uibModalInstance.dismiss("cancel")
          }
        },
      })

      modalInstance.result.then(
        function (result) {
          // Handle 'OK' button click
          console.log("Config Name: " + $scope.selected.configName)
          console.log("Selected Option: " + $scope.service.selectedOption)
          console.log("includePeers Option: " + $scope.service.includePeers)
          console.log("configType Option: " + $scope.service.configType)
          msg.send("setConfig", $scope.selected.configName)
          if ($scope.service.selectedOption == "default") {
            msg.send("saveDefault", $scope.selected.configName, $scope.service.defaultServiceName, $scope.service.configType, $scope.service.includePeers)
          } else {
            msg.sendTo("runtime", "saveConfig", $scope.selected.configName)
          }
          msg.send("getConfigName")
        },
        function () {
          // Handle 'Cancel' button click or modal dismissal
          console.log("Modal dismissed")
        }
      )
    }

    msg.subscribe("getStartYml")
    msg.subscribe("saveDefaults")
    msg.subscribe("getConfigName")
    msg.subscribe("getServiceTypes")
    msg.subscribe("getLocalServices")
    msg.subscribe("registered")
    msg.subscribe("getConnections")
    msg.subscribe("getLocale")
    msg.subscribe("getHosts")
    msg.subscribe("publishStatus")
    msg.subscribe("publishConfigList")
    msg.subscribe("publishInterfaceToNames")

    msg.send("getStartYml")
    msg.send("getConnections")
    msg.send("getServiceTypes")
    msg.send("getLocale")
    msg.send("publishInterfaceToNames")
    msg.send("getConfigName")

    // msg.send("getHosts")
    msg.subscribe(this)
  },
])
