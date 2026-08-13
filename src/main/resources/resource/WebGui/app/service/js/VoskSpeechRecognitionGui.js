angular.module('mrlapp.service.VoskSpeechRecognitionGui', []).controller('VoskSpeechRecognitionGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('VoskSpeechRecognitionGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.recognizedResult = {
        text: null,
        confidence: null,
        isFinal: false
    }
    $scope.log = []
    $scope.partialText = ''
    $scope.isRecording = false
    $scope.micImage = '../WebkitSpeechRecognition/mic.png'
    $scope.modelOptions = []
    $scope.installedModels = []
    $scope.selectedInstalledModel = ''
    $scope.modelToInstall = 'vosk-model-small-en-us-0.15'
    $scope.loadedModelName = null
    $scope.wakeWord = null

    this.updateState = function(service) {
        $scope.service = service
        $scope.isRecording = !!(service.config && service.config.recording)
        $scope.micImage = $scope.isRecording
            ? '../WebkitSpeechRecognition/mic-animate.gif'
            : '../WebkitSpeechRecognition/mic.png'

        if (service.availableModels && service.availableModels.length) {
            $scope.modelOptions = service.availableModels
        }
        if (angular.isArray(service.installedModels)) {
            $scope.installedModels = normalizeInstalled(service.installedModels)
        }
        if (service.config && service.config.model) {
            $scope.selectedInstalledModel = service.config.model
            if (!$scope.modelToInstall) {
                $scope.modelToInstall = service.config.model
            }
        }
        $scope.loadedModelName = loadedNameFromPath(service.loadedModelPath) || (service.config && service.config.model) || null

        if (service.config && service.config.wakeWord) {
            service.wakeWord = service.config.wakeWord
        }
    }

    function normalizeInstalled(list) {
        if (!list || !list.length) {
            return []
        }
        var out = []
        for (var i = 0; i < list.length; i++) {
            var m = list[i]
            if (m == null) {
                continue
            }
            if (typeof m === 'string') {
                out.push({ name: m, language: 'Installed', size: '', label: m, installed: true })
            } else if (m.name) {
                out.push(m)
            }
        }
        return out
    }

    function loadedNameFromPath(path) {
        if (!path) {
            return null
        }
        var parts = path.replace(/\\/g, '/').split('/')
        return parts[parts.length - 1] || null
    }

    $scope.service = mrl.getService($scope.service.name)
    if ($scope.service) {
        _self.updateState($scope.service)
    }

    $scope.isBusy = function() {
        var s = ($scope.service && $scope.service.status) ? $scope.service.status : ''
        return s.indexOf('loading') === 0 || s.indexOf('downloading') === 0 || s.indexOf('extracting') === 0
    }

    $scope.isInstalled = function(name) {
        if (!name || !$scope.installedModels) {
            return false
        }
        for (var i = 0; i < $scope.installedModels.length; i++) {
            if ($scope.installedModels[i].name === name) {
                return true
            }
        }
        return false
    }

    $scope.changeListeningState = function() {
        if (!$scope.isRecording) {
            msg.send('startListening')
        } else {
            msg.send('stopListening')
            msg.send('stopRecording')
        }
    }

    $scope.loadInstalledModel = function() {
        if ($scope.selectedInstalledModel) {
            msg.send('setModel', $scope.selectedInstalledModel)
        }
    }

    $scope.installModel = function() {
        if ($scope.modelToInstall) {
            msg.send('installModel', $scope.modelToInstall)
        }
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
        case 'onInstalledModelInfo':
            $scope.installedModels = normalizeInstalled(data)
            $scope.$apply()
            break
        case 'onInstallModel':
            // installModel() has returned — pull the scanned disk list
            msg.send('getInstalledModelInfo')
            break
        case 'onSetModel':
            msg.send('getInstalledModelInfo')
            break
        case 'onListeningEvent':
            if (data) {
                if (data.isSpeaking && data.confidence) {
                    data.text = 'heard while speaking : ' + data.text
                } else if (data.isSpeaking) {
                    data.text = 'speaking : ' + data.text
                }
                if (data.isFinal) {
                    $scope.recognizedResult = {
                        text: data.text,
                        confidence: data.confidence,
                        isFinal: true
                    }
                    $scope.partialText = ''
                } else if (data.text && !data.isSpeaking) {
                    $scope.partialText = data.text
                }
                $scope.log.unshift(data)
            }
            $scope.$apply()
            break
        case 'onRecognized':
            $scope.recognizedResult = {
                text: data,
                confidence: $scope.recognizedResult.confidence,
                isFinal: true
            }
            $scope.partialText = ''
            $scope.$apply()
            break
        default:
            console.debug('VoskSpeechRecognitionGui unhandled method ' + inMsg.method)
            break
        }
    }

    msg.subscribe('getAvailableModels')
    msg.subscribe('getInstalledModelInfo')
    msg.subscribe('publishInstalledModelInfo')
    msg.subscribe('publishListeningEvent')
    msg.subscribe('publishRecognized')
    msg.subscribe(this)
    msg.send('getInstalledModelInfo')
    msg.send('getAvailableModels')
    msg.send('broadcastState')
}
])
