angular.module('mrlapp.service.Py4jGui', []).controller('Py4jGuiCtrl', ['$scope', 'mrl', '$uibModal', '$timeout', function($scope, mrl, $uibModal, $timeout) {
    console.info('Py4jGuiCtrl')
    var _self = this
    var msg = this.msg

    // list of client keys
    // cant come from service.clients 
    // because its non serializable
    var clients = []

    $scope.scriptCount = 0
    $scope.lastStatus = null
    $scope.log = ''

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
            $scope.log = data + $scope.log
            $scope.$apply()
            break
        case 'onAppend':
            $scope.log = data + $scope.log
            $scope.$apply()
            break
        case 'onClients':
            $scope.clients = data
            $scope.$apply()
            break
        case 'onStatus':
            $scope.lastStatus = data
            if (data.level == 'error') {
                $scope.log = data.detail + '\n' + $scope.log
            }
            console.info("onStatus ", data)
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + msg.method)
            break
        }
    }

    $scope.openScript = function(filename) {
        msg.send('openScript', filename)
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
        msg.send('closing script', scriptName)
        $scope.scriptCount--
    }

    $scope.exec = function() {
        activeScript = $scope.service.openedScripts[$scope.activeKey]
        msg.send('exec', activeScript.code)
    }
    $scope.tabSelected = function(script) {
        console.info('here')
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
            templateUrl: 'py4jFilename.html',
            controller: function($scope, $uibModalInstance) {
                $scope.ok = function() {
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

    msg.subscribe('publishStdOut')
    msg.subscribe('publishAppend')
    msg.subscribe('getClients')
    msg.send('getClients')
    msg.subscribe(this)
}
])
