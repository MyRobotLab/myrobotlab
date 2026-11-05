angular.module("mrlapp.service.RuntimeGui", []).controller("RuntimeGuiCtrl", [
  "$scope",
  "mrl",
  "statusSvc",
  "$timeout",
  "$uibModal",
  "modalService",
  "$http",
  function ($scope, mrl, statusSvc, $timeout, $uibModal, modalService, $http) {
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
          scheduleGraphRefresh()
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
          scheduleGraphRefresh()
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

    // Force-directed message flow graph. Nodes are services, edges are subscriptions.
    $scope.graph = {
      registeredOnly: true,
      query: "",
      status: "loading subscriptions…",
      selected: null,
      selectedEdge: null,
      serviceCount: 0,
      edgeCount: 0,
    }

    var graphHost = null
    var graphRenderer = null
    var graphScene = null
    var graphCamera = null
    var graphRaycaster = null
    var graphNodes = {}
    var graphEdges = []
    var graphPickables = []
    var graphRaw = null
    var graphRunning = false
    var graphRefreshTimer = null
    var graphSimSteps = 0
    var graphDragging = null
    var graphPointer = { x: 0, y: 0, down: false, moved: false }
    var graphOrbit = { theta: 0.5, phi: 1.05, radius: 32, dragging: false }
    var graphLook = null
    var graphYAxis = null
    var graphSphere = null
    var graphCone = null
    var graphUnbind = null
    var graphViewSize = { w: 0, h: 0 }

    function graphColor(typeName) {
      var text = typeName || "service"
      var hash = 0
      for (var i = 0; i < text.length; i++) {
        hash = (hash * 31 + text.charCodeAt(i)) >>> 0
      }
      return new THREE.Color().setHSL((hash % 360) / 360, 0.58, 0.55).getHex()
    }

    function graphLabelSprite(text) {
      var canvas = document.createElement("canvas")
      var ctx = canvas.getContext("2d")
      ctx.font = "28px sans-serif"
      var width = Math.min(512, Math.max(64, Math.ceil(ctx.measureText(text).width) + 24))
      canvas.width = width
      canvas.height = 64
      ctx.font = "28px sans-serif"
      ctx.fillStyle = "#e8eef2"
      ctx.textAlign = "center"
      ctx.textBaseline = "middle"
      ctx.fillText(text, width / 2, 32)
      var texture = new THREE.Texture(canvas)
      texture.needsUpdate = true
      var sprite = new THREE.Sprite(new THREE.SpriteMaterial({ map: texture, transparent: true }))
      sprite.scale.set(Math.max(2.2, width / 40), 1.3, 1)
      return sprite
    }

    function graphKnown(name) {
      return !!mrl.getService(name)
    }

    function graphType(name) {
      var svc = mrl.getService(name)
      return svc && svc.simpleName ? svc.simpleName : ""
    }

    function graphDisplay(name) {
      var shortName = mrl.getShortName(name) || name
      if (name && name.indexOf("@") > 0 && mrl.getFullName(shortName) !== name) {
        return name
      }
      return shortName
    }

    function clearGraphScene() {
      if (!graphScene) {
        return
      }
      graphEdges.forEach(function (edge) {
        if (edge.line) {
          graphScene.remove(edge.line)
          edge.line.geometry.dispose()
          if (edge.line.material) {
            edge.line.material.dispose()
          }
        }
        if (edge.arrow) {
          graphScene.remove(edge.arrow)
          if (edge.arrow.material) {
            edge.arrow.material.dispose()
          }
        }
      })
      Object.keys(graphNodes).forEach(function (id) {
        var node = graphNodes[id]
        graphScene.remove(node.mesh)
        graphScene.remove(node.label)
        if (node.mesh.material) {
          node.mesh.material.dispose()
        }
        if (node.label.material) {
          if (node.label.material.map) {
            node.label.material.map.dispose()
          }
          node.label.material.dispose()
        }
      })
      graphNodes = {}
      graphEdges = []
      graphPickables = []
    }

    function buildSelection(id) {
      var node = graphNodes[id]
      if (!node) {
        return null
      }
      return {
        id: node.id,
        label: node.labelText,
        type: node.type,
        out: node.out,
        inn: node.inn,
      }
    }

    function buildEdgeSelection(edge) {
      if (!edge || !graphNodes[edge.source] || !graphNodes[edge.target]) {
        return null
      }
      return {
        from: edge.source,
        to: edge.target,
        fromLabel: graphNodes[edge.source].labelText,
        toLabel: graphNodes[edge.target].labelText,
        methods: edge.methods.slice(),
      }
    }

    function sameEdge(edge, selected) {
      return selected && edge.source === selected.from && edge.target === selected.to
    }

    function applyGraphStyle() {
      var query = ($scope.graph.query || "").toLowerCase()
      var selectedId = $scope.graph.selected ? $scope.graph.selected.id : null
      var selectedEdge = $scope.graph.selectedEdge
      var neighbor = {}
      if (selectedId && graphNodes[selectedId]) {
        neighbor[selectedId] = true
        graphNodes[selectedId].out.forEach(function (edge) {
          neighbor[edge.to] = true
        })
        graphNodes[selectedId].inn.forEach(function (edge) {
          neighbor[edge.from] = true
        })
      } else if (selectedEdge) {
        neighbor[selectedEdge.from] = true
        neighbor[selectedEdge.to] = true
      }
      Object.keys(graphNodes).forEach(function (id) {
        var node = graphNodes[id]
        var dim = query && node.labelText.toLowerCase().indexOf(query) < 0 && id.toLowerCase().indexOf(query) < 0
        var active = (!selectedId && !selectedEdge) || neighbor[id]
        node.mesh.material.opacity = dim ? 0.12 : active ? 1 : 0.28
        node.label.material.opacity = dim ? 0.08 : active ? 1 : 0.25
        var scale = id === selectedId ? 1.45 : 1
        node.mesh.scale.set(scale, scale, scale)
      })
      graphEdges.forEach(function (edge) {
        var chosen = sameEdge(edge, selectedEdge)
        var hot = chosen || (selectedId && (edge.source === selectedId || edge.target === selectedId))
        var dim = query && graphNodes[edge.source] && graphNodes[edge.target] && graphNodes[edge.source].mesh.material.opacity < 0.2 && graphNodes[edge.target].mesh.material.opacity < 0.2
        var color = chosen ? 0xfff1a8 : hot ? 0xf0c14a : 0x6ea8c9
        edge.line.material.color.setHex(color)
        edge.arrow.material.color.setHex(color)
        edge.line.material.opacity = dim ? 0.05 : hot || (!selectedId && !selectedEdge) ? 0.9 : 0.12
        edge.arrow.material.opacity = edge.line.material.opacity
      })
    }

    function placeGraphNodes(ids) {
      var count = ids.length
      ids.forEach(function (id, index) {
        var typeName = graphType(id)
        var radius = 0.55
        var geometry = graphSphere
        var material = new THREE.MeshBasicMaterial({
          color: graphColor(typeName),
          transparent: true,
          opacity: 1,
        })
        var mesh = new THREE.Mesh(geometry, material)
        var angle = (index / Math.max(count, 1)) * Math.PI * 2
        var ring = 6 + (index % 5) * 1.4
        mesh.position.set(Math.cos(angle) * ring, ((index % 7) - 3) * 0.8, Math.sin(angle) * ring)
        mesh.userData.nodeId = id
        var labelText = graphDisplay(id)
        var label = graphLabelSprite(labelText)
        label.position.copy(mesh.position)
        label.position.y += 1.35
        graphScene.add(mesh)
        graphScene.add(label)
        graphNodes[id] = {
          id: id,
          type: typeName,
          labelText: labelText,
          mesh: mesh,
          label: label,
          radius: radius,
          pos: mesh.position,
          vel: new THREE.Vector3(),
          out: [],
          inn: [],
        }
        graphPickables.push(mesh)
      })
    }

    function addGraphEdge(sourceId, targetId, method, callback) {
      var pair = null
      for (var i = 0; i < graphEdges.length; i++) {
        if (graphEdges[i].source === sourceId && graphEdges[i].target === targetId) {
          pair = graphEdges[i]
          break
        }
      }
      if (!pair) {
        var geometry = new THREE.Geometry()
        geometry.vertices.push(new THREE.Vector3(), new THREE.Vector3())
        var material = new THREE.LineBasicMaterial({ color: 0x6ea8c9, transparent: true, opacity: 0.85 })
        var line = new THREE.Line(geometry, material)
        var arrowMat = new THREE.MeshBasicMaterial({ color: 0x6ea8c9, transparent: true, opacity: 0.9 })
        var arrow = new THREE.Mesh(graphCone, arrowMat)
        graphScene.add(line)
        graphScene.add(arrow)
        pair = {
          source: sourceId,
          target: targetId,
          line: line,
          arrow: arrow,
          methods: [],
          dir: new THREE.Vector3(),
          start: new THREE.Vector3(),
          end: new THREE.Vector3(),
        }
        graphEdges.push(pair)
      }
      pair.methods.push({ method: method, callback: callback })
      var record = {
        from: sourceId,
        to: targetId,
        fromLabel: graphNodes[sourceId].labelText,
        toLabel: graphNodes[targetId].labelText,
        method: method,
        callback: callback,
      }
      graphNodes[sourceId].out.push(record)
      graphNodes[targetId].inn.push(record)
    }

    function rebuildMessageGraphFrom(data) {
      if (!graphScene) {
        return
      }
      clearGraphScene()
      var ids = {}
      var publishers = data && angular.isObject(data) && !angular.isArray(data) ? data : {}
      Object.keys(publishers).forEach(function (publisher) {
        var publisherId = mrl.getFullName(publisher)
        if (!$scope.graph.registeredOnly || graphKnown(publisherId)) {
          ids[publisherId] = true
        }
        var methods = publishers[publisher] || {}
        Object.keys(methods).forEach(function (method) {
          var listeners = angular.isArray(methods[method]) ? methods[method] : []
          listeners.forEach(function (listener) {
            if (!listener || !listener.callbackName) {
              return
            }
            var subscriberId = mrl.getFullName(listener.callbackName)
            if (!$scope.graph.registeredOnly || graphKnown(subscriberId)) {
              ids[subscriberId] = true
            }
          })
        })
      })
      if ($scope.graph.registeredOnly && mrl.getServices) {
        mrl.getServices().forEach(function (svc) {
          if (svc && svc.name) {
            ids[mrl.getFullName(svc)] = true
          }
        })
      }
      var idList = Object.keys(ids).sort()
      placeGraphNodes(idList)
      var subscriptionCount = 0
      Object.keys(publishers).forEach(function (publisher) {
        var publisherId = mrl.getFullName(publisher)
        if (!graphNodes[publisherId]) {
          return
        }
        var methods = publishers[publisher] || {}
        Object.keys(methods).forEach(function (method) {
          var listeners = angular.isArray(methods[method]) ? methods[method] : []
          listeners.forEach(function (listener) {
            if (!listener || !listener.callbackName) {
              return
            }
            var subscriberId = mrl.getFullName(listener.callbackName)
            if (!graphNodes[subscriberId] || subscriberId === publisherId) {
              if (graphNodes[subscriberId] && subscriberId === publisherId) {
                graphNodes[publisherId].out.push({
                  from: publisherId,
                  to: subscriberId,
                  fromLabel: graphNodes[publisherId].labelText,
                  toLabel: graphNodes[publisherId].labelText,
                  method: method,
                  callback: listener.callbackMethod,
                })
                graphNodes[publisherId].inn.push(graphNodes[publisherId].out[graphNodes[publisherId].out.length - 1])
                subscriptionCount++
              }
              return
            }
            addGraphEdge(publisherId, subscriberId, method, listener.callbackMethod)
            subscriptionCount++
          })
        })
      })
      idList.forEach(function (id) {
        var degree = graphNodes[id].out.length + graphNodes[id].inn.length
        graphNodes[id].radius = 0.42 + Math.min(0.55, degree * 0.015)
        graphNodes[id].mesh.scale.set(graphNodes[id].radius / 0.55, graphNodes[id].radius / 0.55, graphNodes[id].radius / 0.55)
      })
      graphSimSteps = 0
      $scope.graph.serviceCount = idList.length
      $scope.graph.edgeCount = subscriptionCount
      $scope.graph.status = idList.length + " services, " + subscriptionCount + " subscriptions"
      if ($scope.graph.selected && !graphNodes[$scope.graph.selected.id]) {
        $scope.graph.selected = null
      } else if ($scope.graph.selected) {
        $scope.graph.selected = buildSelection($scope.graph.selected.id)
      }
      if ($scope.graph.selectedEdge) {
        var kept = null
        for (var edgeIndex = 0; edgeIndex < graphEdges.length; edgeIndex++) {
          if (sameEdge(graphEdges[edgeIndex], $scope.graph.selectedEdge)) {
            kept = graphEdges[edgeIndex]
            break
          }
        }
        $scope.graph.selectedEdge = buildEdgeSelection(kept)
      }
      applyGraphStyle()
    }

    function stepGraph() {
      var ids = Object.keys(graphNodes)
      if (!ids.length || graphSimSteps > 420) {
        return
      }
      graphSimSteps++
      var repulsion = 18
      var spring = 0.045
      var rest = 8
      for (var i = 0; i < ids.length; i++) {
        var a = graphNodes[ids[i]]
        if (a === graphDragging) {
          continue
        }
        for (var j = i + 1; j < ids.length; j++) {
          var b = graphNodes[ids[j]]
          var dx = a.pos.x - b.pos.x
          var dy = a.pos.y - b.pos.y
          var dz = a.pos.z - b.pos.z
          var dist2 = dx * dx + dy * dy + dz * dz + 0.35
          var force = repulsion / dist2
          var scale = force / Math.sqrt(dist2)
          a.vel.x += dx * scale
          a.vel.y += dy * scale
          a.vel.z += dz * scale
          if (b !== graphDragging) {
            b.vel.x -= dx * scale
            b.vel.y -= dy * scale
            b.vel.z -= dz * scale
          }
        }
        a.vel.x += -a.pos.x * 0.012
        a.vel.y += -a.pos.y * 0.012
        a.vel.z += -a.pos.z * 0.012
      }
      graphEdges.forEach(function (edge) {
        var source = graphNodes[edge.source]
        var target = graphNodes[edge.target]
        var dx = target.pos.x - source.pos.x
        var dy = target.pos.y - source.pos.y
        var dz = target.pos.z - source.pos.z
        var dist = Math.sqrt(dx * dx + dy * dy + dz * dz) || 1
        var force = (dist - rest) * spring
        var fx = (dx / dist) * force
        var fy = (dy / dist) * force
        var fz = (dz / dist) * force
        if (source !== graphDragging) {
          source.vel.x += fx
          source.vel.y += fy
          source.vel.z += fz
        }
        if (target !== graphDragging) {
          target.vel.x -= fx
          target.vel.y -= fy
          target.vel.z -= fz
        }
      })
      ids.forEach(function (id) {
        var node = graphNodes[id]
        if (node === graphDragging) {
          node.vel.set(0, 0, 0)
          return
        }
        node.vel.multiplyScalar(0.72)
        node.pos.add(node.vel)
      })
    }

    function syncGraphObjects() {
      Object.keys(graphNodes).forEach(function (id) {
        var node = graphNodes[id]
        node.label.position.copy(node.pos)
        node.label.position.y += 1.15 + node.radius
      })
      graphEdges.forEach(function (edge) {
        var source = graphNodes[edge.source].pos
        var target = graphNodes[edge.target].pos
        edge.dir.copy(target).sub(source)
        var length = edge.dir.length() || 1
        edge.dir.multiplyScalar(1 / length)
        var sourceRadius = graphNodes[edge.source].radius
        var targetRadius = graphNodes[edge.target].radius
        edge.start.copy(source).add(edge.dir.clone().multiplyScalar(sourceRadius + 0.15))
        edge.end.copy(target).sub(edge.dir.clone().multiplyScalar(targetRadius + 0.55))
        edge.line.geometry.vertices[0].copy(edge.start)
        edge.line.geometry.vertices[1].copy(edge.end)
        edge.line.geometry.verticesNeedUpdate = true
        edge.arrow.position.copy(edge.end)
        if (edge.dir.dot(graphYAxis) < -0.98) {
          edge.dir.x += 0.05
          edge.dir.normalize()
        }
        edge.arrow.quaternion.setFromUnitVectors(graphYAxis, edge.dir)
      })
    }

    function updateGraphCamera() {
      var phi = Math.max(0.12, Math.min(Math.PI - 0.12, graphOrbit.phi))
      graphOrbit.phi = phi
      graphCamera.position.set(
        graphLook.x + graphOrbit.radius * Math.sin(phi) * Math.sin(graphOrbit.theta),
        graphLook.y + graphOrbit.radius * Math.cos(phi),
        graphLook.z + graphOrbit.radius * Math.sin(phi) * Math.cos(graphOrbit.theta)
      )
      graphCamera.lookAt(graphLook)
    }

    function graphPointerToRay(event) {
      var rect = graphRenderer.domElement.getBoundingClientRect()
      var mouse = new THREE.Vector2(
        ((event.clientX - rect.left) / rect.width) * 2 - 1,
        -((event.clientY - rect.top) / rect.height) * 2 + 1
      )
      graphRaycaster.setFromCamera(mouse, graphCamera)
    }

    function pickGraphNode(event) {
      graphPointerToRay(event)
      var hits = graphRaycaster.intersectObjects(graphPickables, false)
      if (!hits.length) {
        return null
      }
      return graphNodes[hits[0].object.userData.nodeId] || null
    }

    function projectGraphPoint(vector) {
      var projected = vector.clone()
      projected.project(graphCamera)
      var rect = graphRenderer.domElement.getBoundingClientRect()
      return {
        x: (projected.x * 0.5 + 0.5) * rect.width,
        y: (-projected.y * 0.5 + 0.5) * rect.height,
        behind: projected.z < -1 || projected.z > 1,
      }
    }

    function distanceToSegment(px, py, ax, ay, bx, by) {
      var abx = bx - ax
      var aby = by - ay
      var length2 = abx * abx + aby * aby || 1
      var t = ((px - ax) * abx + (py - ay) * aby) / length2
      t = Math.max(0, Math.min(1, t))
      var dx = px - (ax + abx * t)
      var dy = py - (ay + aby * t)
      return Math.sqrt(dx * dx + dy * dy)
    }

    function pickGraphEdge(event) {
      var rect = graphRenderer.domElement.getBoundingClientRect()
      var px = event.clientX - rect.left
      var py = event.clientY - rect.top
      var best = null
      var bestDistance = 14
      graphEdges.forEach(function (edge) {
        var start = projectGraphPoint(edge.start)
        var end = projectGraphPoint(edge.end)
        if (start.behind && end.behind) {
          return
        }
        var distance = distanceToSegment(px, py, start.x, start.y, end.x, end.y)
        if (distance < bestDistance) {
          bestDistance = distance
          best = edge
        }
      })
      return best
    }

    function resizeGraph() {
      if (!graphHost || !graphRenderer) {
        return
      }
      var width = graphHost.clientWidth
      var height = graphHost.clientHeight
      if (width < 2 || height < 2) {
        return
      }
      if (graphViewSize.w !== width || graphViewSize.h !== height) {
        graphViewSize.w = width
        graphViewSize.h = height
        graphRenderer.setSize(width, height, false)
        graphCamera.aspect = width / height
        graphCamera.updateProjectionMatrix()
      }
    }

    function renderGraph() {
      if (!graphRunning) {
        return
      }
      requestAnimationFrame(renderGraph)
      resizeGraph()
      if (!graphHost || graphHost.clientWidth < 2) {
        return
      }
      stepGraph()
      syncGraphObjects()
      updateGraphCamera()
      graphRenderer.render(graphScene, graphCamera)
    }

    function bindGraphInput() {
      var dom = graphRenderer.domElement
      function preventMenu(event) {
        event.preventDefault()
      }
      function onMouseDown(event) {
        graphPointer.x = event.clientX
        graphPointer.y = event.clientY
        graphPointer.down = true
        graphPointer.moved = false
        var node = pickGraphNode(event)
        if (node && event.button === 0) {
          graphDragging = node
          graphSimSteps = Math.min(graphSimSteps, 200)
        } else {
          graphOrbit.dragging = true
        }
      }
      function onMouseUp() {
        var wasDrag = graphPointer.moved
        var dragged = graphDragging
        graphPointer.down = false
        graphOrbit.dragging = false
        graphDragging = null
        if (!wasDrag && dragged) {
          $scope.$applyAsync(function () {
            $scope.graph.selected = buildSelection(dragged.id)
            $scope.graph.selectedEdge = null
            applyGraphStyle()
          })
        }
      }
      function onMouseMove(event) {
        if (!graphPointer.down) {
          return
        }
        var dx = event.clientX - graphPointer.x
        var dy = event.clientY - graphPointer.y
        if (dx * dx + dy * dy > 9) {
          graphPointer.moved = true
        }
        graphPointer.x = event.clientX
        graphPointer.y = event.clientY
        if (graphDragging) {
          graphPointerToRay(event)
          var plane = new THREE.Plane()
          var normal = new THREE.Vector3()
          normal.copy(graphCamera.position).sub(graphLook).normalize()
          plane.setFromNormalAndCoplanarPoint(normal, graphDragging.pos)
          var hit = graphRaycaster.ray.intersectPlane(plane)
          if (hit) {
            graphDragging.pos.copy(hit)
            graphDragging.vel.set(0, 0, 0)
            graphSimSteps = Math.min(graphSimSteps, 280)
          }
        } else if (graphOrbit.dragging) {
          graphOrbit.theta -= dx * 0.008
          graphOrbit.phi -= dy * 0.008
        }
      }
      function onWheel(event) {
        event.preventDefault()
        graphOrbit.radius *= event.deltaY > 0 ? 1.08 : 0.92
        graphOrbit.radius = Math.max(6, Math.min(120, graphOrbit.radius))
      }
      function onClick(event) {
        if (graphPointer.moved) {
          return
        }
        var node = pickGraphNode(event)
        var edge = node ? null : pickGraphEdge(event)
        $scope.$applyAsync(function () {
          $scope.graph.selected = node ? buildSelection(node.id) : null
          $scope.graph.selectedEdge = edge ? buildEdgeSelection(edge) : null
          applyGraphStyle()
        })
      }
      dom.addEventListener("contextmenu", preventMenu)
      dom.addEventListener("mousedown", onMouseDown)
      dom.addEventListener("wheel", onWheel, { passive: false })
      dom.addEventListener("click", onClick)
      window.addEventListener("mouseup", onMouseUp)
      window.addEventListener("mousemove", onMouseMove)
      graphUnbind = function () {
        dom.removeEventListener("contextmenu", preventMenu)
        dom.removeEventListener("mousedown", onMouseDown)
        dom.removeEventListener("wheel", onWheel)
        dom.removeEventListener("click", onClick)
        window.removeEventListener("mouseup", onMouseUp)
        window.removeEventListener("mousemove", onMouseMove)
      }
    }

    function startMessageGraph(host) {
      if (typeof THREE === "undefined") {
        $scope.graph.status = "three.js is not loaded"
        return
      }
      graphHost = host
      graphScene = new THREE.Scene()
      graphCamera = new THREE.PerspectiveCamera(50, 1.6, 0.1, 400)
      graphLook = new THREE.Vector3(0, 0, 0)
      graphYAxis = new THREE.Vector3(0, 1, 0)
      graphRenderer = new THREE.WebGLRenderer({ antialias: true, alpha: false })
      graphRenderer.setClearColor(0x1b1e24, 1)
      graphRenderer.setPixelRatio(window.devicePixelRatio || 1)
      graphRenderer.domElement.style.width = "100%"
      graphRenderer.domElement.style.height = "100%"
      graphRenderer.domElement.style.display = "block"
      host.appendChild(graphRenderer.domElement)
      graphRaycaster = new THREE.Raycaster()
      graphSphere = new THREE.SphereGeometry(0.55, 18, 14)
      // r73 has CylinderGeometry, not ConeGeometry. radiusTop 0 makes an arrowhead along +Y.
      graphCone = new THREE.CylinderGeometry(0, 0.16, 0.48, 8)
      graphRunning = true
      bindGraphInput()
      renderGraph()
    }

    function bootMessageGraph(tries) {
      var host = document.getElementById("runtime-message-graph")
      if (!host) {
        if (tries > 0) {
          $timeout(function () {
            bootMessageGraph(tries - 1)
          }, 200)
        }
        return
      }
      var attached = graphRenderer && graphRenderer.domElement && host.contains(graphRenderer.domElement)
      if (!attached) {
        if (graphRenderer) {
          graphRunning = false
          try {
            graphRenderer.dispose()
          } catch (e) {
            console.info("message graph renderer dispose", e)
          }
          graphRenderer = null
        }
        try {
          startMessageGraph(host)
        } catch (e) {
          console.error("message graph failed to start", e)
          $scope.graph.status = "message graph failed to start"
          return
        }
      }
      if (!graphRaw) {
        $scope.refreshMessageGraph()
      }
    }

    $scope.rebuildMessageGraph = function () {
      rebuildMessageGraphFrom(graphRaw || {})
    }

    $scope.applyGraphQuery = function () {
      applyGraphStyle()
    }

    $scope.selectGraphNode = function (id) {
      if (!graphNodes[id]) {
        return
      }
      $scope.graph.selected = buildSelection(id)
      $scope.graph.selectedEdge = null
      applyGraphStyle()
    }

    $scope.openGraphService = function (id) {
      mrl.changeTab(id)
    }

    $scope.refreshMessageGraph = function () {
      $scope.graph.status = "loading subscriptions…"
      $http.get("/api/service/runtime/getNotifyEntries").then(
        function (response) {
          graphRaw = response.data || {}
          $scope.rebuildMessageGraph()
        },
        function () {
          $scope.graph.status = "could not load subscriptions"
        }
      )
    }

    function scheduleGraphRefresh() {
      if (graphRefreshTimer) {
        $timeout.cancel(graphRefreshTimer)
      }
      graphRefreshTimer = $timeout(function () {
        $scope.refreshMessageGraph()
      }, 800)
    }

    $scope.$on("$destroy", function () {
      graphRunning = false
      if (graphUnbind) {
        graphUnbind()
      }
      if (graphRefreshTimer) {
        $timeout.cancel(graphRefreshTimer)
      }
      if (graphRenderer) {
        if (graphRenderer.domElement && graphRenderer.domElement.parentNode) {
          graphRenderer.domElement.parentNode.removeChild(graphRenderer.domElement)
        }
        graphRenderer.dispose()
      }
    })

    $timeout(function () {
      bootMessageGraph(30)
    }, 0)

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
