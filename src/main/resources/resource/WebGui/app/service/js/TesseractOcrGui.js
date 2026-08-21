angular.module('mrlapp.service.TesseractOcrGui', []).controller('TesseractOcrGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('TesseractOcrGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.imagePath = 'src/test/resources/OpenCV/i_am_a_droid.jpg'
    $scope.dirty = false
    $scope.lastOcr = null
    $scope.psmPresets = [
        { name: 'auto (document)', value: 3 },
        { name: 'block', value: 6 },
        { name: 'line (signs)', value: 7 },
        { name: 'word', value: 8 },
        { name: 'char', value: 10 },
        { name: 'sparse', value: 11 },
        { name: 'raw line', value: 13 }
    ]
    $scope.langModels = [
        { code: 'eng', label: 'eng — English' },
        { code: 'fra', label: 'fra — French' },
        { code: 'deu', label: 'deu — German' },
        { code: 'spa', label: 'spa — Spanish' },
        { code: 'ita', label: 'ita — Italian' },
        { code: 'por', label: 'por — Portuguese' },
        { code: 'nld', label: 'nld — Dutch' },
        { code: 'pol', label: 'pol — Polish' },
        { code: 'tur', label: 'tur — Turkish' },
        { code: 'rus', label: 'rus — Russian' },
        { code: 'ara', label: 'ara — Arabic' },
        { code: 'hin', label: 'hin — Hindi' },
        { code: 'chi_sim', label: 'chi_sim — Chinese simplified' },
        { code: 'chi_tra', label: 'chi_tra — Chinese traditional' },
        { code: 'jpn', label: 'jpn — Japanese' },
        { code: 'kor', label: 'kor — Korean' }
    ]

    this.updateState = function(service) {
        $scope.service = service
        if (service.lastResult) {
            $scope.lastOcr = service.lastResult
        }
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onOcr':
            $scope.lastOcr = data
            $scope.$apply()
            break
        case 'onText':
            if (!$scope.lastOcr) {
                $scope.lastOcr = { text: data }
            }
            $scope.$apply()
            break
        default:
            console.error('ERROR - unhandled method ' + $scope.name + ' ' + inMsg.method)
            break
        }
    }

    $scope.saveConfig = function() {
        msg.send('apply', $scope.service.config)
        msg.send('save')
        $scope.dirty = false
    }

    $scope.runOcr = function() {
        if (!$scope.imagePath) {
            return
        }
        msg.send('recognize', $scope.imagePath)
    }

    $scope.installLang = function() {
        if ($scope.service.config && $scope.service.config.lang) {
            msg.send('installLang', $scope.service.config.lang)
        }
    }

    $scope.service = mrl.getService($scope.service.name)
    if ($scope.service) {
        _self.updateState($scope.service)
    }

    msg.subscribe('publishOcr')
    msg.subscribe('publishText')
    msg.subscribe(this)
}])
