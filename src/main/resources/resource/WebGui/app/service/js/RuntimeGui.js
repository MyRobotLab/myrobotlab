angular.module('mrlapp.service.RuntimeGui', []).controller('RuntimeGuiCtrl', ['$scope', 'mrl', 'statusSvc', '$timeout', '$uibModal', 'modalService', function($scope, mrl, statusSvc, $timeout, $uibModal, modalService) {
    console.info('RuntimeGuiCtrl')
    var _self = this
    var msg = this.msg

    var statusMaxSize = 2500

    this.updateState = function(service) {
        $scope.service = service
        $scope.locale.selected = service.locale.language
        $scope.localeTag.selected = service.locale
        /*
        service.serviceData.categoryTypes["show all"] = {
            "name": "show all",
            "serviceTypes": []
        }
        */
    }

    $scope.locales = {}

    $scope.platform = $scope.service.platform
    $scope.status = ""
    $scope.cmd = ""
    $scope.registry = {}
    $scope.connections = {}
    $scope.newName = null
    $scope.newType = ""
    $scope.heartbeatTs = null
    $scope.hosts = []

    $scope.languages = {
        'en': {
            'language': 'en',
            'displayLanguage': 'English'
        }
    }

    $scope.locale = {
        selected: null
    }

    $scope.localeTag = {
        'selected': {
            'tag': 'en-US'
        }
    }

    $scope.category = {
        selected: null
    }

    $scope.categoryServiceTypes = null

    $scope.disabled = undefined;
    $scope.person = {};

    var msgKeys = {}

    let categoryServiceTypes = null

    // $scope.categoryServiceTypes = $scope.service.serviceData.categoryTypes[$scope.category.selected].serviceTypes

    $scope.filterServices = function() {
        var result = {};
        // console.debug('$scope.category.selected is ' + $scope.category.selected)
        const entries = Object.entries($scope.service.serviceData.serviceTypes)

        if ($scope.category.selected != null && ($scope.category.selected == 'show all')) {
            return $scope.service.serviceData.serviceTypes
        }

        for (const [fullTypeName,metaData] of entries) {
            // if (metaData.simpleName.toLowerCase().includes($scope.newType)) {

            if ($scope.category.selected != null) {
                categoryServiceTypes = $scope.service.serviceData.categoryTypes[$scope.category.selected].serviceTypes
            } else {
                categoryServiceTypes = null
            }

            if (/*metaData.simpleName.toLowerCase().includes($scope.newType) && */
            categoryServiceTypes != null && categoryServiceTypes.includes(metaData.name)) {
                result[fullTypeName] = metaData;
            }
        }
        return result;
    }

    // FIXME - maintain contextPath !!!
    $scope.sendToCli = function(cmd) {
        console.log("sendToCli " + cmd)
        $scope.cmd = ""
        contextPath = null
        msg.send("sendToCli", "runtime@" + mrl.getId(), cmd)
    }

    $scope.setServiceType = function(serviceType) {
        $scope.newType = serviceType
    }

    $scope.setConfigName = function() {
        console.info('setConfigName')
        if ($scope.selectedConfig.length > 0) {
            $scope.service.configName = $scope.selectedConfig[0]
            msg.sendTo('runtime', 'setConfigName', $scope.service.configName)
        }
    }

    $scope.start = function() {

        if ($scope.newName == null) {
            mrl.error("name of service is required")
            return
        }
        if ($scope.newType == null) {
            mrl.error("type of service is required")
            return
        }

        if (typeof $scope.newType == 'object') {
            $scope.newType = $scope.newType.name
        }
        msg.send('start', $scope.newName, $scope.newType)

        $scope.newName = null;
        $scope.newType = null;
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            break
        case 'onLocalServices':
            $scope.registry = data
            //  $scope.$apply()
            break
        case 'onLocale':
            $scope.locale.selected = data.language
            $scope.$apply()
            break
        case 'onLocales':
            ls = data
            unique = {}
            $scope.service.locales = {}
            // new Set()
            for (const key in ls) {
                if (ls[key].displayLanguage) {
                    // unique.add(ls[key].displayLanguage)
                    // unique.push(ls[key].language)
                    unique[ls[key].language] = {
                        'language': ls[key].language,
                        'displayLanguage': ls[key].displayLanguage
                    }
                }
                // $scope.service.locales[key] =ls[key] 
            }
            // $scope.languages = Array.from(unique)
            $scope.languages = unique
            $scope.locales = ls
            // it is transient in java to reduce initial registration payload
            // $scope.service.locales = ls
            $scope.$apply()
            break

        case 'onConfigList':
            $scope.service.configList = data.sort()
            $scope.$apply()
            break

        case 'onServiceTypes':

            $scope.possibleServices = data
            mrl.setPossibleServices($scope.possibleServices)
            break

        case 'onAttachMatrix':
            console.info('onAttachMatrix', data)
                for (const [type,interfaze] of Object.entries(data)) {
                // console.info(requestor, interfaze)
                let servicesMatchingType = []
                for (const [name,service] of Object.entries(mrl.getRegistry())) {
                    if (service.serviceClass == type){
                        servicesMatchingType.push(name)
                    }
                }
                for (let j = 0; j < servicesMatchingType.length; ++j){
                    let requestor = servicesMatchingType[j]
                    for (const [inter,targetServices] of Object.entries(interfaze)) {
                        targetServices.sort()
                        // for (const possibleService of targetServices) {
                            // console.info(requestor, interfaze, possibleService)
                            // get panel/controller - onMessage ?
                            let panel = mrl.getPanel(requestor)
                            const method = 'on' + inter.substring(inter.lastIndexOf('.') + 1)
                            let msg = mrl.createMessage(requestor, method, [interfaze[inter]])
                            try {
                                // WTF - this callback was always available ?
                                if (panel){
                                    panel.scope.guictrl.onMsg(msg)
                                }                            
                            } catch (e) {
                                console.error(e)
                            }
                        //}
                    }
                }
            }
            // mrl.getPanel()
            break

        case 'onRegistered':
            console.log("onRegistered")
            break

        case 'onConnections':
            $scope.connections = data
            $scope.$apply()
            break

        case 'onHosts':
            $scope.hosts = data
            $scope.$apply()
            break

        case 'onStatus':
            $scope.status = data.name + ' ' + data.level + ' ' + data.detail + "\n" + $scope.status
            if ($scope.status.length > 300) {
                $scope.status = $scope.status.substring(0, statusMaxSize)
            }
            break

        case 'onCli':
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

        case 'onReleased':
            console.info("runtime - onRelease" + data)
            break

        case 'onHeartbeat':
            let heartbeat = data
            let hb = heartbeat.name + '@' + heartbeat.id + ' sent onHeartbeat - ';
            $scope.heartbeatTs = heartbeat.ts
            $scope.$apply()

            for (let i in heartbeat.serviceList) {
                let serviceName = heartbeat.serviceList[i].name + '@' + heartbeat.serviceList[i].id
                hb += serviceName + ' '

                // FIXME - 'merge' ie remove missing services

                // FIXME - want to maintain "local" registry ???
                // currently maintaining JS process registry - should the RuntimeGui also maintain
                // its 'own' sub-registry ???
                if (!serviceName in mrl.getRegistry()) {
                    // 
                    console.warn(serviceName + ' not defined in registry - sending registration request');
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

    $scope.shutdown = function(type) {
        var modalInstance = $uibModal.open({
            //animation: true,
            // templateUrl: 'nav/shutdown.html',
            // template: '<div class="modal-header"> HELLO ! </div>',
            // controller: $scope.doShutdown,
            // controller: 'RuntimeGuiCtrl',
            scope: $scope,
            // controller: 'ModalController',

            animation: true,
            templateUrl: 'nav/shutdown.html',
            controller: 'shutdownCtrl2',

            resolve: {
                type: function() {
                    return type
                }
            }
        })
        console.info('shutdown ' + modalInstance)
    }

    $scope.setAllLocales = function(locale) {
        console.info(locale)
    }

    $scope.startConfig = function() {
        console.info('startConfig')
        if ($scope.selectedConfig.length) {
            for (let i = 0; i < $scope.selectedConfig.length; ++i) {
                msg.sendTo('runtime', 'load', 'data/config/' + $scope.selectedConfig[i] + '/runtime.yml')
            }
        }
    }

    $scope.releaseConfig = function() {
        console.info('releaseConfig')
        if ($scope.selectedConfig && $scope.selectedConfig.length) {
            for (let i = 0; i < $scope.selectedConfig.length; ++i) {
                msg.sendTo('runtime', 'releaseConfig', 'data/config/' + $scope.selectedConfig[i] + '/runtime.yml')
            }
        }
    }

    $scope.saveConfig = function() {
        console.info('saveConfig')

        let onOK = function() {
            msg.sendTo('runtime', 'setConfigName', $scope.service.configName)
            msg.sendTo('runtime', 'save', $scope.service.configDir + '/' + $scope.service.configName + "/runtime.yml")
        }

        let onCancel = function() {
            console.info('save config cancelled')
        }

        let ret = modalService.openOkCancel('widget/modal-dialog.view.html', 'Save Configuration', 'Save your current configuration in a directory named', onOK, onCancel, $scope);
        console.info('ret ' + ret);
    }

    // $scope.possibleServices = Object.values(mrl.getPossibleServices())
    msg.subscribe("getServiceTypes")
    msg.subscribe("getLocalServices")
    msg.subscribe("registered")
    msg.subscribe("getConnections")
    msg.subscribe("getLocale")
    msg.subscribe("getLocales")
    msg.subscribe("getHosts")
    msg.subscribe("publishStatus")
    msg.subscribe('publishConfigList')

    msg.subscribe('publishAttachMatrix')

    //msg.send("getLocalServices")
    msg.send("getConnections")
    msg.send("getServiceTypes")
    msg.send("getLocale")
    msg.send("getLocales")
    // msg.send("getHosts")
    msg.subscribe(this)
}
])
