angular.module('mrlapp.service.RasPiGui', []).controller('RasPiGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('RasPiGuiCtrl')
    var _self = this
    var msg = this.msg

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
        case 'onPinDefinition':
            $scope.service.pinIndex[data.pin] = data
            $scope.$apply()
            break
        case 'onPinArray':
            for (const pd of data){
                $scope.service.pinIndex[pd.pin].value = pd.value
            }            
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.scan = function(bus){
        msg.send('scan', bus)
    }

    $scope.write = function(pinDef){
        msg.send('write', pinDef.pin, pinDef.valueDisplay?1:0)
    }

    $scope.pinMode = function(pinDef) {
        console.info(pinDef)
        // FIXME - standardize interface with Arduino :(
        msg.send('pinMode', pinDef.pin, pinDef.mode)
    }

    msg.subscribe('publishPinDefinition')
    msg.subscribe('publishPinArray')
    msg.subscribe(this)
}
])
.filter('toArray', function() {
    return function(obj) {
      if (obj instanceof Object) {
        return Object.keys(obj).map(function(key) {
          return obj[key];
        });
      } else {
        return obj;
      }
    };
  });