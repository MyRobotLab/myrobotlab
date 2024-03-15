angular.module('mrlapp.service.Py4jGui', []).controller('Py4jGuiCtrl', ['$scope', 'mrl', '$uibModal', '$timeout', function($scope, mrl, $uibModal, $timeout) {
    console.info('Py4jGuiCtrl')
    var _self = this
    var msg = this.msg

    // list of client keys
    // cant come from service.clients 
    // because its non serializable
    var clients = []

    // filesystem list of scripts
    $scope.scriptList = []

    // this UI's currently active script
    $scope.activeKey = null

    _self.updateState = function(service) {
        $scope.service = service
    }

    this.onMsg = function(msg) {
        let data = msg.data[0]
        switch (msg.method) {
            // FIXME - bury it ?
        case 'onState':
            // its important to externalize the updating
            // of the service body in a method rather than doing the 
            // updates inline here - because when things are first initialized
            // we want to call the same method - and if it was inline that
            // would make a mess
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onStdOut':
            if (data !== "\n") {
                $scope.service.logs.unshift(data)
                if ($scope.service.logs.length > 300) {
                    $scope.service.logs.pop()
                }
                $scope.$apply()
            }
            break
        case 'onAppend':
            $scope.$apply()
            break
        case 'onScriptList':
            $scope.scriptList = data
            $scope.$apply()
            break
        case 'onClients':
            $scope.clients = data
            $scope.$apply()
            break
        case 'onStatus':
            if (data.level == 'error') {
                $scope.service.logs.unshift(data.detail)
            }
            console.info("onStatus ", data)
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + msg.method)
            break
        }
    }

    //----- ace editors related callbacks begin -----//
    $scope.aceLoaded = function(e) {
        console.info("ace loaded")
    }

    $scope.aceChanged = function(e) {
        console.info("ace changed")
        activeScript = $scope.service.openedScripts[$scope.activeKey]
        msg.send('updateScript', activeScript.file, activeScript.code)
    }

    $scope.closeScript = function(scriptName) {
        // FIXME - save first ?
        msg.send('closeScript', scriptName)
    }

    $scope.exec = function() {
        activeScript = $scope.service.openedScripts[$scope.activeKey]
        msg.send('exec', activeScript.code)
    }
    $scope.tabSelected = function(script) {
        console.info('tabSelected')
        $scope.activeKey = script.file
    }

    $scope.saveScript = function() {
        activeScript = $scope.service.openedScripts[$scope.activeKey]
        msg.send('saveScript', activeScript.file, activeScript.code)
    }

    $scope.getPossibleServices = function(item) {
        ret = Object.values(mrl.getPossibleServices())
        return ret
    }

    $scope.addScript = function() {
        var modalInstance = $uibModal.open({
            templateUrl: 'addScript.html',
            controller: function($scope, $uibModalInstance) {
                $scope.ok = function() {
                    if (!$scope.filename) {
                        console.error('filename cannot be null')
                        return
                    }

                    msg.send('addScript', $scope.filename, '# new awesome robot script\n')
                    $uibModalInstance.close($scope.filename)
                }

                $scope.cancel = function() {
                    $uibModalInstance.dismiss('cancel')
                }

                $scope.checkEnterKey = function(event) {
                    if (event.keyCode === 13) {
                        $scope.ok()
                    }
                }

            },
            size: 'sm'
        })

        modalInstance.result.then(function(filename) {
            // Do something with the filename
            console.log("Filename: ", filename)
        }, function() {
            // Modal dismissed
            console.log("Modal dismissed")
        })
    }

    $scope.installPackage = function() {
        var modalInstance = $uibModal.open({
            templateUrl: 'installPackage.html',
            controller: function($scope, $uibModalInstance) {
                $scope.ok = function() {
                    if (!$scope.packageName) {
                        console.error('filename cannot be null')
                        return
                    }

                    msg.send('installPipPackages', [$scope.packageName])
                    $uibModalInstance.close($scope.packageName)
                }

                $scope.cancel = function() {
                    $uibModalInstance.dismiss('cancel')
                }

                $scope.checkEnterKey = function(event) {
                    if (event.keyCode === 13) {
                        $scope.ok()
                    }
                }

            },
            size: 'sm'
        })

        modalInstance.result.then(function(filename) {
            // Do something with the filename
            console.log("Filename: ", filename)
        }, function() {
            // Modal dismissed
            console.log("Modal dismissed")
        })
    }

    $scope.openScript = function() {

        msg.send('getScriptList')

        var modalInstance = $uibModal.open({
            templateUrl: 'openScript.html',
            scope: $scope,
            controller: function($scope, $uibModalInstance) {
                $scope.ok = function(file) {
                    msg.send('openScript', file)
                    $uibModalInstance.close()
                }

                $scope.cancel = function() {
                    $uibModalInstance.dismiss('cancel')
                }

                $scope.checkEnterKey = function(event) {
                    if (event.keyCode === 13) {
                        $scope.ok()
                    }
                }

            },
            size: 'sm'
        })

        modalInstance.result.then(function(filename) {
            // Do something with the filename
            console.log("Filename: ", filename)
        }, function() {
            // Modal dismissed
            console.log("Modal dismissed")
        })
    }

    $scope.clear = function() {
        msg.send('clear')
    }

    msg.subscribe('publishStdOut')
    msg.subscribe('publishAppend')
    msg.subscribe('getClients')
    msg.subscribe('getScriptList')
    msg.send('getClients')
    msg.send('getScriptList')
    msg.subscribe(this)
}
])
