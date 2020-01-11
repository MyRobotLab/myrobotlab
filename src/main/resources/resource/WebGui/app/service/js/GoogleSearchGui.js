angular.module('mrlapp.service.GoogleSearchGui', [])
.controller('GoogleSearchGuiCtrl', ['$scope', '$log', 'mrl', '$uibModal', '$sce', function($scope, $log, mrl, $uibModal, $sce) { // $modal ????
    $log.info('GoogleSearchGuiCtrl')
    // grab the self and message
    let _self = this
    let msg = this.msg
    
    $scope.rows = []
    $scope.searchText = null


    // following the template.
    this.updateState = function(service) {
        // use another scope var to transfer/merge selection
        // from user - service.currentSession is always read-only
        // all service data should never be written to, only read from
      
        $scope.service = service
    }

    $scope.search = function(){

        $scope.rows.unshift($scope.searchText)
        msg.send('search', $scope.searchText)
        $scope.searchText = ''
    }
    
    
    this.onMsg = function(inMsg) {
        $log.info("GoogleSearchGui.onMsg(" + inMsg.method +')')
        let data = inMsg.data[0]

        switch (inMsg.method) {
        
        case 'onState':
            _self.updateState(data)
            $scope.properties = mrl.getProperties(data)
            $scope.$apply()
            break

        case 'onText':
            var textData = data
            $scope.rows.unshift(data)
            $scope.$apply()
            break
       
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
        
    }
    
    
    // subscribe to the response from programab.
    
    msg.subscribe('publishText')
    msg.subscribe(this)
}
])