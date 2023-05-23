angular.module('mrlapp.service.SolrGui', []).controller('SolrGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('SolrGuiCtrl');
    var _self = this
    var msg = this.msg
    // TODO: something useful?!
    $scope.solrResults = '';
    $scope.queryString = '*:*';
    	
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
      $scope.service = service
    }

    this.onMsg = function(inMsg) {
      let data = inMsg.data[0]
      switch (inMsg.method) {
        case 'onResults': 
    	  var solrResults = data;
    	  console.info("On Results!");
          $scope.solrResults = solrResults;
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
    
    $scope.search = function(querystring) {
      console.info('SolrGuiCtrl - Search Clicked!' + querystring);
      mrl.sendTo($scope.service.name, "search", querystring);
    };
    
    msg.subscribe('publishResults');
    msg.subscribe(this);
    // mrl.subscribe($scope.service.name, 'publishResults', $scope.service.results);
    // $scope.panel.initDone();
                
  }]);