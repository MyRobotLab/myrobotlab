angular.module('mrlapp.service.DocumentPipelineGui', []).controller('DocumentPipelineGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('FileConnectorGuiCtrl');
    var _self = this
    var msg = this.msg
  
    $scope.document = '';
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
      $scope.service = service
    }

    this.onMsg = function(inMsg) {
      let data = inMsg.data[0]
      switch (inMsg.method) {
        case 'onDocument': 
    	  console.info("On Document!");
    	  $scope.document = data;
          $scope.$apply();
          break
        case 'onState':
          _self.updateState(data)
          $scope.$apply()
          break	
        case 'onStatus':
          $scope.status = data;
          $scope.$apply()
          break
        default:
    	  console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
    	  break
      }
    
    };
    
    // This could result in a lot of data getting returned to the webgui.. we'll see.
    // msg.subscribe('publishDocument');
    msg.subscribe(this);
                
  }]);