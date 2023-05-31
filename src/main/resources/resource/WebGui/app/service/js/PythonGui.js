angular.module('mrlapp.service.PythonGui', []).controller('PythonGuiCtrl', ['$scope', 'mrl', '$uibModal', function($scope, mrl, $uibModal) {
    console.info('PythonGuiCtrl')
    var _self = this
    var msg = this.msg
    var firstTime = true
    var name = $scope.name
    var newFileDialog = null
    $scope.output = ''
    $scope.activeTabIndex = 0
    $scope.scriptCount = 0
    $scope.activeScript = null
    $scope.scripts = {}
    $scope.openingScript = true
    $scope.dropdownIsOpen = true
    $scope.lastStatus = null
    $scope.log = ''

    // 2 dialogs 
    $scope.loadFile = false

    _self.updateState = function(service) {
        
        $scope.service = service
        $scope.scriptCount = 0

        $scope.scripts = {}
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

        // create a new script if no scripts are currently opened
        if (Object.keys(service.openedScripts).length == 0){
            // msg.send('openScript', 'script-' + $scope.getFormattedDataTime() + '.py', '# new cool robot script\n')
            firstTime = false
        }
    }

    this.onMsg = function(msg) {
        let data = msg.data[0]
        switch (msg.method) {
        case 'onState':
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
        case 'onStatus':
            $scope.lastStatus = data
            if (data.level == 'error'){
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
        console.info("ace loaded")
        $scope.activeTabIndex = $scope.scriptCount
    }

    $scope.aceChanged = function(e) {
        console.info("ace changed")
        msg.send('updateScript', $scope.activeScript.file, $scope.activeScript.code)
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
        msg.send('closeScript', scriptName)
        msg.broadcastState()
        // console.log("removed " + scriptName)
    }

    $scope.exec = function() {
        // non-blocking exec
        msg.send('exec', $scope.activeScript.code, false)
    }
    $scope.tabSelected = function(script) {
        console.info('here')
        $scope.activeScript = script
        // need to get a handle on hte tab's ui / text
        // $scope.editors.setValue(script.code)
    }

    $scope.getTabHeader = function(key) {
        return $scope.getName(key)
        //return key.substr(key.lastIndexOf('/') + 1)
    }

    $scope.saveScript = function() {
        msg.send('saveScript', $scope.activeScript.file, $scope.activeScript.code)
    }

    $scope.downloadScript = function() {
        var textFileAsBlob = new Blob([$scope.activeScript.code],{
            type: 'text/plain'
        })
        var downloadLink = document.createElement("a")
        downloadLink.download = $scope.getName($scope.activeScript.file)
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

    $scope.getFormattedDataTime = function(){

        const currentDate = new Date();        
        const month = String(currentDate.getMonth() + 1).padStart(2, '0'); // Add leading zero if necessary
        const day = String(currentDate.getDate()).padStart(2, '0'); // Add leading zero if necessary
        const hour = String(currentDate.getHours()).padStart(2, '0'); // Add leading zero if necessary
        const minute = String(currentDate.getMinutes()).padStart(2, '0'); // Add leading zero if necessary
        const formattedDateTime = `${month}-${day}-${hour}-${minute}`;        
        console.log("Formatted Date and Time:", formattedDateTime); 
        return formattedDateTime
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

    msg.subscribe('publishStdOut')
    msg.subscribe('publishAppend')
    msg.subscribe(this)

    $scope.openScript = function(filename, code){
        msg.send('openScript', filename, code)
    }


     $scope.newFile = function() {
        newFileDialog = $uibModal.open({
            // template: '<h3 class="modal-title">New File</h3>',
            templateUrl: "newFile.html",
            scope: $scope,
            controller: function($scope) {
                $scope.cancel = function() {
                    newFileDialog.dismiss()
                }
            }
        })        
     }
    
}
]).directive('fileModel', ['$parse', function($parse) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            var model = $parse(attrs.fileModel)
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
