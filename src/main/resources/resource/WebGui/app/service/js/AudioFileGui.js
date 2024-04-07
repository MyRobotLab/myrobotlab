angular.module('mrlapp.service.AudioFileGui', []).controller('AudioFileGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('AudioFileGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.peak = 0
    $scope.peakMax = 0

    // playing paused stopped
    $scope.activity = null

    // $scope.playFile = function() {        
    //     msg.send('playFile', $scope.selectedFile)
    // }

    $scope.play = function() {
        // if (blah){
        // $scope.selectedFile = selectedFiles[0]    
        // } else {
        //     $scope.selectedFile = selectedFiles[0]    
        // }
        let playFile = $scope.selectedFile
        msg.send('play', $scope.selectedFile)

    }

    $scope.setSelectedFileFromTrack = function(selected) {
        $scope.selectedFile = selected
    }

    $scope.startPlaylist = function() {
        if ($scope.selectedPlaylist) {
            msg.send('startPlaylist', $scope.selectedPlaylist[0])
        } else {
            msg.send('startPlaylist')
        }
    }

    $scope.stopPlaylist = function() {
        if ($scope.selectedPlaylist) {
            msg.send('stopPlaylist', $scope.selectedPlaylist[0])
            msg.send('stop')
        } else {
            msg.send('stopPlaylist')
            msg.send('stop')
        }
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.service.loudness = 20
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

        if (!$scope.inputSelectedFile){
            $scope.inputSelectedFile = $scope.selectedFile
        }

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
            $scope.$apply()
            break
        case 'onAudioEnd':
            $scope.playing = data.filename
            $scope.activity = 'stopped'
            $scope.$apply()
            $scope.service.lastPlayed = data.filename
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
