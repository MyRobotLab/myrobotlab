angular.module('mrlapp.service.PythonGui', []).controller('PythonGuiCtrl', ['$log', '$scope', 'mrl', '$uibModal', '$timeout', function($log, $scope, mrl, $uibModal, $timeout) {
    $log.info('PythonGuiCtrl')
    var _self = this
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

    // 2 dialogs 
    $scope.loadFile = false
    $scope.newFile = false

    _self.updateState = function(service) {
        $scope.service = service
        $scope.scriptCount = 0

        angular.forEach(service.openedScripts, function(value, key) {
            if (!angular.isDefined($scope.scripts[key])) {
                $scope.scripts[key] = value
            }
            $scope.scriptCount++
        })
        // this doesn't work - its the ace-ui callback that 
        // changes the activeTabIndex
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

    $scope.newScript = function(filename, script) {
        if (!script) {
            script = '# new awesome robot script\n'
        }
        msg.send('openScript', filename, script)
        $scope.newName = ''
        // clear input text
        $scope.newFile = false
        // close dialog
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
        $scope.activeTabIndex = $scope.scriptCount
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
        $scope.scriptCount--
        delete $scope.scripts[scriptName]
        console.log("removed " + scriptName)
    }

    $scope.exec = function() {
        // non-blocking exec
        msg.send('exec', $scope.activeScript.code, false)
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

    $scope.getPossibleServices = function(item) {
        ret = Object.values(mrl.getPossibleServices())
        return ret
    }

    $scope.export = function() {
        msg.send('exportAll')
    }

    $scope.uploadFile = function() {

        var f = $scope.myFile;
        var r = new FileReader();

        r.onloadend = function(e) {
            var data = e.target.result;
            console.info('onloadend')
            $scope.newScript(f.name, data)
            $scope.loadFile = false
            // close dialog
        }

        r.readAsBinaryString(f);
        console.info('readAsBinaryString')
    }

    // $scope.possibleServices = Object.values(mrl.getPossibleServices())
    msg.subscribe('publishStdOut')
    msg.subscribe(this)
    msg.send('newScript')
}
]).directive('fileModel', ['$parse', function($parse) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            var model = $parse(attrs.fileModel);
            var modelSetter = model.assign;

            element.bind('change', function() {
                scope.$apply(function() {
                    modelSetter(scope, element[0].files[0]);
                });
            });
        }
    };
}
]);
