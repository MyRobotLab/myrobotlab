angular.module('mrlapp.service.FileConnectorGui', []).controller('FileConnectorGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('FileConnectorGuiCtrl');
    var _self = this
    var msg = this.msg
    
    $scope.filepath = '';
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
    
    $scope.startCrawling = function() {		
		console.info("Start crawling>" + $scope.filepath + "<")
    	mrl.sendTo($scope.service.name, "setDirectory", $scope.filepath)
    	mrl.sendTo($scope.service.name, "startCrawling");
    }

    $scope.stopCrawling = function() {
    	// TODO: this doesn't seem to work.
    	console.info("Stop crawling")
    	mrl.sendTo($scope.service.name, "stopCrawling");
    }
    
    // This could result in a lot of data getting returned to the webgui.. we'll see.
    // msg.subscribe('publishDocument');
    msg.subscribe(this);
                
  }]);