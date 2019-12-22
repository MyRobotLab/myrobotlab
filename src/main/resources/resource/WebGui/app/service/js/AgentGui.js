angular.module('mrlapp.service.AgentGui', [])
.controller('AgentGuiCtrl', ['$scope', '$log', 'mrl', '$uibModal', function($scope, $log, mrl, $uibModal) {
    $log.info('AgentGuiCtrl')
    var _self = this
    var msg = this.msg
    
    // init variables
    $scope.processes = {}

    $scope.isRunning = function(processData){
        return (processData.state == 'running')
    }
    
    $scope.start = function(size) {
        
        var modalInstance = $uibModal.open({
            animation: $scope.animationsEnabled,
            template: "<div>start instances</div>",
            // templateUrl: 'widget/start.html',
            controller: 'startCtrl',
            size: size,
            resolve: {
                items: function() {
                    return $scope.items
                }
            }
        })
        
        modalInstance.result.then(function(selectedItem) {
            $scope.selected = selectedItem
        }, function() {
            $log.info('Modal dismissed at: ' + new Date())
        })
    }
    
    // FIXME - do ws connections - and report heartbeat
    $scope.stopTimer = function(sectionId) {
        var section = document.getElementById(sectionId)
        if (section != null ) {
            section.getElementsByTagName('timer')[0].stop()
        }
    }
    
    $scope.startTimer = function(sectionId) {
        //var x = angular.element( document.querySelector( '#blah' ) )
        var section = document.getElementById(sectionId)
        if (section != null ) {
            section.getElementsByTagName('timer')[0].start()
        }
    }
    
    $scope.autoUpdate = function(name, b) {
        msg.send('autoUpdate', name, !b)
    }
    
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service    
        $scope.processes = service.processes    
    }
    
    
    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break       
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }
    
    msg.subscribe('publishVersions')
    msg.subscribe(this)
}
])
