angular.module('mrlapp.service.AudioFileGui', []).controller('AudioFileGuiCtrl', ['$log', '$scope', 'mrl', function($log, $scope, mrl) {
    $log.info('AudioFileGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.selectedFile = null

    $scope.treedata = [{
        "label": "User",
        "id": "role1",
        "children": [{
            "label": "subUser1",
            "id": "role11",
            "children": []
        }, {
            "label": "subUser2",
            "id": "role12",
            "children": [{
                "label": "subUser2-1",
                "id": "role121",
                "children": [{
                    "label": "subUser2-1-1",
                    "id": "role1211",
                    "children": []
                }, {
                    "label": "subUser2-1-2",
                    "id": "role1212",
                    "children": []
                }]
            }]
        }]
    }, {
        "label": "Admin",
        "id": "role2",
        "children": []
    }, {
        "label": "Guest",
        "id": "role3",
        "children": []
    }];

    $scope.$watch('abc.currentNode', function(newObj, oldObj) {
        if ($scope.abc && angular.isObject($scope.abc.currentNode)) {
            console.log('Node Selected!!');
            console.log($scope.abc.currentNode);
        }
    }, false);

    $scope.fileNameChanged = function(ele) {
        var files = ele.files
        var l = files.length
        var namesArr = []

        for (var i = 0; i < l; i++) {
            namesArr.push(files[i].name)
        }

        msg.send('playFile', files[0].name)
    }

    $scope.playFile = function() {
        msg.send('playFile', $scope.selectedFile)
    }

    // init

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.stopCapture = service.stopCapture
        $scope.soundCaptured = service.soundCaptured
        $scope.captureAudio = service.captureAudio
        $scope.stopAudioFile = service.stopAudioFile
        $scope.playAudio = service.playAudio
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        default:
            $log.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    }

    msg.subscribe(this)
}
])
