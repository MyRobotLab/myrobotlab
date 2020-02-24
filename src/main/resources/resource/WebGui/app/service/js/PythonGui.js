angular.module('mrlapp.service.PythonGui', []).controller('PythonGuiCtrl', ['$log', '$scope', 'mrl', '$uibModal', '$timeout', function($log, $scope, mrl, $uibModal, $timeout) {
    $log.info('PythonGuiCtrl')
    _self = this
    var msg = this.msg
    var name = $scope.name
    // init scope values
    // $scope.service = mrl.getService(name)
    $scope.output = ''
    $scope.activeTabIndex = 0
    $scope.scriptCount = 0
    $scope.activeScript = null
    $scope.scripts = {}
    $scope.test = null
    $scope.openingScript = true
    $scope.dropdownIsOpen = true
    $scope.lastStatus = null
    $scope.log = ''

    this.updateState = function(service) {
        $scope.service = service
        $scope.scriptCount = 0

        angular.forEach(service.openedScripts, function(value, key) {
            if (!angular.isDefined($scope.scripts[key])) {
                $scope.scripts[key] = value
            }
            $scope.scriptCount++
        })

        $scope.activeTabIndex = $scope.scriptCount
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
        case 'onStatus':
            $scope.lastStatus = data
            $scope.$apply()
            break
        default:
            $log.error("ERROR - unhandled method " + msg.method)
            break
        }
    }

    // utility methods //
    // gets script name from full path name
    $scope.getName = function(path) {
        if (path.indexOf("/") >= 0) {
            return (path.split("/").pop())
        }
        if (path.indexOf("\\") >= 0) {
            return (path.split("\\").pop())
        }
        return path
    }

    //----- ace editors related callbacks begin -----//
    $scope.aceLoaded = function(e) {
        $log.info("ace loaded")
    }

    $scope.aceChanged = function(e) {
        $log.info("ace changed")
    }
    //----- ace editors related callbacks end -----//
    $scope.addScript = function() {
        let scriptName = 'Untitled-' + $scope.scriptCount + 1
        var newScript = {
            name: scriptName,
            code: ''
        }
        $scope.scripts[scriptName] = newScript
        console.log($scope.activeTabIndex)
    }

    $scope.closeScript = function(scriptName) {
        // FIXME - save first ?
        msg.send('closeScript', scriptName)
        delete $scope.scripts[scriptName]
        $scope.scriptCount--
        console.log("removed " + scriptName)
    }

    $scope.exec = function() {
        msg.send('exec', $scope.activeScript.code)
    }
    $scope.tabSelected = function(script) {
        $log.info('here')
        $scope.activeScript = script
        // need to get a handle on hte tab's ui / text
        // $scope.editors.setValue(script.code)
    }

    $scope.getTabHeader = function(key) {
        return $scope.getName(key)
        //return key.substr(key.lastIndexOf('/') + 1)
    }

    $scope.saveTextAsFile = function() {
        var textFileAsBlob = new Blob([$scope.activeScript.code],{
            type: 'text/plain'
        })
        var downloadLink = document.createElement("a")
        downloadLink.download = $scope.getName($scope.activeScript.file.path)
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
    }

    $scope.item = "blah"

    $scope.edit = function(item) {

        var itemToEdit = item

        $dialogs.dialog(angular.extend(dialogOptions, {
            resolve: {
                item: angular.copy(itemToEdit)
            }
        })).open().then(function(result) {
            if (result) {
                angular.copy(result, itemToEdit)
            }
            itemToEdit = undefined
        })
    }

    $scope.getPossibleServices = function(item) {
        return Object.values(mrl.getPossibleServices())
    }

    $scope.export = function(){
        msg.send('exportAll')
    }

    // $scope.possibleServices = Object.values(mrl.getPossibleServices())
    msg.subscribe('publishStdOut')
    msg.subscribe(this)
}
])
