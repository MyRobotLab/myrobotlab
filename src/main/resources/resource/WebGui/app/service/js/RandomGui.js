angular.module('mrlapp.service.RandomGui', []).controller('RandomGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('RandomGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.state = {
        showServices: false,
        showMethods: false,
        showMethodEntries: false,
        selectedService: null,
        selectedMethod: null
    }

    $scope.serviceList = null
    $scope.methodList = null
    $scope.methodEntryList = null

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onServiceList':
            $scope.serviceList = data
            $scope.$apply()
            break
        case 'onMethodsFromName':
            $scope.methodList = data
            $scope.$apply()
            break
        case 'onMethodQuery':
            $scope.methodEntryList = data
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.getServiceList = function() {
        $scope.state.showServices = true
        msg.send('getServiceList')
    }

    $scope.addService = function(name) {
        $scope.state.selectedService = name
        $scope.state.showMethods = true
        msg.send('getMethodsFromName', name)
    }

    $scope.addMethod = function(name) {
        $scope.state.selectedMethod = name
        $scope.state.showMethodEntries = true
        msg.send('methodQuery', $scope.state.selectedService, name)
        // $scope.state.showMethods = true
    }

    $scope.addMethodEntries = function(name) {
        // $scope.state.selectedMethod = name
        // msg.send('methodQuery', name)
        // $scope.state.showMethods = true
    }

    msg.subscribe('getServiceList')
    msg.subscribe('getMethodsFromName')
    msg.subscribe('methodQuery')
    msg.send('getServiceList')
    msg.subscribe(this)
}
])
