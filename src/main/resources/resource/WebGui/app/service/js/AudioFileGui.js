angular.module('mrlapp.service.AudioFileGui', []).controller('AudioFileGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('AudioFileGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.peak = 0
    $scope.peakMax = 0
    var firstUpdate = true

    // playing paused stopped
    $scope.activity = null

    $scope.play = function() {
        let playFile = $scope.selectedFile
        msg.send('play', $scope.selectedFile)
    }

    $scope.setSelectedFileFromTrack = function(selected) {
        $scope.selectedFile = selected
    }

    $scope.startPlaylist = function() {
        if ($scope.selectedPlaylist) {
            msg.send('startPlaylist', $scope.selectedPlaylist, $scope.service.config.shuffle, $scope.service.config.repeat)
        } else {
            msg.send('startPlaylist')
        }
    } 
    $scope.skip = function() {
            msg.send('skip')
    }

    $scope.stopPlaylist = function() {
            msg.send('stopPlaylist')
            msg.send('stop')
    }

    $scope.setPlaylist = function(name){
        console.info('setPlaylist ' + name)
        msg.send('setPlaylist', name)
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.service.loudness = 20

        if (firstUpdate){
            $scope.selectedPlaylist = $scope.service.config.currentPlaylist
            
            firstUpdate = false
        }
        
        if (!$scope.selectedFile) {

            if (service.lastPlayed) {
                $scope.selectedFile = service.lastPlayed.filename;
            }

            if (service.current) {
                $scope.selectedFile = service.current.filename;
            }
        }

        if (service.current) {
            $scope.playing = service.current.filename;
            $scope.activity = 'playing'
        }

        $scope.selectedFile = service.lastPlayed.filename;
        

    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onAudioStart':
            $scope.playing = data.filename
            $scope.activity = 'playing'
            if (!$scope.selectedFile || $scope.selectedFile == ""){
                $scope.selectedFile = data.filename
            }
            $scope.$apply()
            break
        case 'onAudioEnd':
            $scope.playing = data.filename
            $scope.activity = 'stopped'
            if (!$scope.selectedFile || $scope.selectedFile == ""){
                $scope.selectedFile = data.filename
            }
            $scope.$apply()
            break
        case 'onPeak':
            $scope.peak = Math.round(data/* * 100 */)
            if ($scope.peak > $scope.peakMax){
                $scope.peakMax = $scope.peak
            }
            $scope.$apply()
            break

        default:
            console.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    }

    msg.subscribe('publishAudioStart')
    msg.subscribe('publishAudioEnd')
    msg.subscribe('publishPeak')
    msg.subscribe(this)
}
])
