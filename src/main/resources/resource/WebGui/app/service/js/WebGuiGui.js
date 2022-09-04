angular.module('mrlapp.service.WebGuiGui', []).controller('WebGuiGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
    $log.info('WebGuiGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.mrl = mrl

     // $scope.displayImages =  mrl.getDisplayImages()
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.port = service.port
    }

    // init scope variables
    $scope.pulseData = ''
    //$scope.saveP
    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'display':
            console.info('display - ' + data)
            mrl.display(data)
            $scope.$apply()
            break
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onStatus':
            break            
        case 'onShowAll':
            // panelSvc.showAll(inMsg.data[0]) TODO - fix
            break
        case 'onShow':
            // panelSvc.show(inMsg.data[0]) TODO - fix
            break
        case 'onHide':
            // panelSvc.hide(inMsg.data[0]) TODO - fix
            break
        case 'onPanel':
            // panelSvc.setPanel(inMsg.data[0]) TODO - fix
            break
        default:
            $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    //mrl.subscribe($scope.service.name, 'pulse')
    msg.subscribe('publishShowAll')
    // msg.subscribe('publishHideAll') FIXME ? not symmetric
    msg.subscribe('publishHide')
    msg.subscribe('publishShow')
    msg.subscribe('publishSet')
    msg.subscribe('publishPanel')
    // msg.send("publishPanels")
    // msg.subscribe('loadPanels')
    msg.subscribe(this)
}
])

