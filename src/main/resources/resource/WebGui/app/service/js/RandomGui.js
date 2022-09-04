angular.module('mrlapp.service.RandomGui', []).controller('RandomGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('RandomGuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.state = {
        showServices: false,
        showMethods: false,
        showMethodEntries: false,
        selectedService: null,
        selectedMethod: null,
        minTime: null,
        maxTime: null
    }

    $scope.parameters = {}

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

    $scope.addMethodEntries = function(name) {// $scope.state.selectedMethod = name
    // msg.send('methodQuery', name)
    // $scope.state.showMethods = true
    }

    $scope.addRandom = function(index) {
        console.info(index)
        ranges = []

        rs = $scope.parameters[index];
        for (var key in rs) {
            ranges.push(rs[key])
        }

        ranges.push()
        args = []
        // args.push('addRandom')
        args.push($scope.state.minTime)
        args.push($scope.state.maxTime)
        args.push($scope.state.selectedService)
        args.push($scope.state.selectedMethod)
        args.push(ranges)

        msg.sendArgs('addRandom', args)

        $scope.state.selectedService = null
        $scope.state.selectedMethod = null
    }

    $scope.remove = function(key) {
        msg.send('remove', key)
        msg.send('broadcastState')
        
    }


    msg.subscribe('getServiceList')
    msg.subscribe('getMethodsFromName')
    msg.subscribe('methodQuery')
    msg.send('getServiceList')
    msg.subscribe(this)
}
])
