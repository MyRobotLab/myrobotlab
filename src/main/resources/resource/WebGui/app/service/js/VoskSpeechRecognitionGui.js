angular.module('mrlapp.service.VoskSpeechRecognitionGui', []).controller('VoskSpeechRecognitionGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('VoskSpeechRecognitionGuiCtrl')
    var _self = this
    var msg = this.msg

    this.updateState = function(service) {
        $scope.service = service
        if (service && service.availableModels && service.availableModels.length) {
            $scope.modelOptions = service.availableModels
        }
        if (service && service.config && service.config.model) {
            $scope.modelToInstall = service.config.model
        }
    }

    $scope.service = mrl.getService($scope.service.name)
    $scope.lastText = ''
    $scope.partialText = ''
    $scope.modelOptions = ($scope.service.availableModels && $scope.service.availableModels.length)
        ? $scope.service.availableModels
        : []
    $scope.modelToInstall = ($scope.service.config && $scope.service.config.model)
        ? $scope.service.config.model
        : 'vosk-model-small-en-us-0.15'

    // If state arrived before availableModels was populated, request a refresh
    if (!$scope.modelOptions.length) {
        msg.send('getAvailableModels')
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onAvailableModels':
            $scope.modelOptions = data || []
            $scope.$apply()
            break
        case 'onListeningEvent':
            if (data) {
                if (data.isFinal) {
                    $scope.lastText = data.text
                    $scope.partialText = ''
                } else {
                    $scope.partialText = data.text
                }
            }
            $scope.$apply()
            break
        case 'onRecognized':
            $scope.lastText = data
            $scope.$apply()
            break
        default:
            console.error('ERROR - unhandled method ' + $scope.name + ' ' + inMsg.method)
            break
        }
    }

    $scope.toggleListening = function() {
        if ($scope.service.config && $scope.service.config.listening) {
            msg.send('stopListening')
            msg.send('stopRecording')
        } else {
            msg.send('startListening')
        }
    }

    $scope.installModel = function() {
        if ($scope.modelToInstall) {
            msg.send('installModel', $scope.modelToInstall)
        }
    }

    $scope.loadModel = function() {
        if ($scope.modelToInstall) {
            msg.send('setModel', $scope.modelToInstall)
        }
    }

    msg.subscribe('getAvailableModels')
    msg.subscribe('publishListeningEvent')
    msg.subscribe('publishRecognized')
    msg.subscribe(this)
}
])
