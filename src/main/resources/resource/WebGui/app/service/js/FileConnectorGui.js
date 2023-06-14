angular.module('mrlapp.service.FileConnectorGui', []).controller('FileConnectorGuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('FileConnectorGuiCtrl');
    var _self = this
    var msg = this.msg
    
    $scope.filepath = 'Z:\\Music'
    $scope.document = '';	
//    // TODO: something useful?!
//    $scope.solrResults = '';
//    $scope.queryString = '*:*';
//    $scope.startOffset = 0;
//    $scope.endOffset = 0;
//    $scope.numFound = 0;
//    $scope.pageSize = 20;
//    $scope.filters = [];
//    // TODO: maybe some other fields..
//    // TODO: support range facets
//    $scope.facetFields = ['type', 'xmpdm_artist', 'xmpdm_releasedate', 'xmpdm_genre','sender_type', 'sender','method'];
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
      $scope.service = service
    }

    this.onMsg = function(inMsg) {
      let data = inMsg.data[0]
      switch (inMsg.method) {
        case 'onDocument': 
    	  //var solrResults = JSON.parse(data);
    	  console.info("On Document!");
    	  $scope.document = data;
    	  //console.info(solrResults);
          //$scope.solrResults = solrResults;
          // set the start/end offsets perhaps?
          //$scope.numFound = solrResults.numFound;
          // TODO: this is conflated logic.
          // $scope.startOffset = solrResults.start
          //$scope.endOffset = solrResults.size + solrResults.start 
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
    
    $scope.startCrawling = function(filepath) {
    	mrl.sendTo($scope.service.name, "startCrawling");
    }
    
    // TODO ?  I don't think we want to subscribe to publishDocument..
    msg.subscribe('publishDocument');
    msg.subscribe(this);
    // mrl.subscribe($scope.service.name, 'publishResults', $scope.service.results);
    // $scope.panel.initDone();
                
  }]);