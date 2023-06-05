angular.module('mrlapp.service.SolrGui', []).controller('SolrGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('SolrGuiCtrl');
    var _self = this
    var msg = this.msg
    // TODO: something useful?!
    $scope.solrResults = '';
    $scope.queryString = '*:*';
    $scope.startOffset = 0;
    $scope.endOffset = 0;
    $scope.numFound = 0;
    	
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
      $scope.service = service
    }

    this.onMsg = function(inMsg) {
      let data = inMsg.data[0]
      switch (inMsg.method) {
        case 'onResults': 
    	  var solrResults = JSON.parse(data);
    	  console.info("On Results!");
    	  console.info(solrResults);
          $scope.solrResults = solrResults;
          // set the start/end offsets perhaps?
          $scope.numFound = solrResults.numFound;
          $scope.startOffset = solrResults.start+1
          $scope.endOffset = solrResults.size + $scope.startOffset 
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
      // TODO: add the facets
       
      mrl.sendTo($scope.service.name, "searchWithFacets", querystring, 10, 0, ['type', 'sender','method']);
    };
    
    msg.subscribe('publishResults');
    msg.subscribe(this);
    // mrl.subscribe($scope.service.name, 'publishResults', $scope.service.results);
    // $scope.panel.initDone();
                
  }]);