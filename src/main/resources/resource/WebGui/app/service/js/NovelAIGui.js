angular.module('mrlapp.service.NovelAIGui', []).controller('NovelAIGuiCtrl', ['peer', '$scope', 'mrl', '$uibModal', function(peer, $scope, mrl, $uibModal) {
    console.info('NovelAIGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.autoClear = true
    $scope.textArea = false
    $scope.spoken = ''

    // new selected voice "container" - since it comes from a map next leaves are
    // key & value ... value contains the entire voice selected
    $scope.newVoice = {
        selected: null
    }

    this.updateState = function(service) {
        $scope.service = service
        if (service.voice) {
            $scope.newVoice.selected = {
                'key': service.voice.name,
                'value': service.voice
            }
        }
        $scope.$apply()
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            break
        case 'onStartSpeaking':
            $scope.spoken = data
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.speak = function(text) {
        msg.send("speak", text)

        if ($scope.autoClear) {
            $scope.text = ''
        }
    }

    $scope.setVoice = function(text) {
        console.log($scope.service.voice.name)
        msg.send("setVoice", text.name)
    }

    msg.subscribe('publishStartSpeaking')
    msg.subscribe(this)
}
])
