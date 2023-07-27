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
    $scope.pageSize = 50;
    $scope.filters = [];
    // TODO: maybe some other fields..
    // TODO: support range facets
    $scope.facetFields = ['type', 'artist_facet', 'album_facet', 'genre_facet', 'year_facet', 'sender_type', 'sender','method'];
    $scope.facetFields = ['type', 'artist_facet', 'album_facet', 'genre_facet', 'year_facet', 'sender_type', 'sender','method', 'content_type_facet'];
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
          // TODO: this is conflated logic.
          // $scope.startOffset = solrResults.start
          $scope.endOffset = solrResults.size + solrResults.start 
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
    
    // start a new search
    $scope.execNewSearch = function() {
      console.info('SolrGuiCtrl - Search Clicked!' + $scope.querystring);
      // this is someone clicking the search button.. we should clear the filters and reset pagination
      $scope.filters = [];
      $scope.startOffset = 0;
      $scope.execSearch();
    };
    
    // run the search based on the current query params selected.
    $scope.execSearch = function() {
      mrl.sendTo($scope.service.name, "searchWithFacets", $scope.queryString, 10, $scope.startOffset, $scope.facetFields, $scope.filters);
    }
    
    $scope.filter = function(field, value) {
    	// add the filter and run the search
    	$scope.filters.push(field + ":\"" + value + "\"");
    	// reset to first page when adding a new filter
    	$scope.startOffset = 0;
    	$scope.execSearch();
    }
    
    $scope.removeFilter = function(filter) {
    	// remove the filter that was passed in.
    	$scope.filters = $scope.filters.filter(e => e !== filter); 
    	$scope.execSearch();
    }
    
    $scope.prevPage = function() {
    	// update the start offset and run the search
    	$scope.startOffset -= $scope.pageSize;
    	if ($scope.startOffset < 0) {
    		$scope.startOffset = 0;
    	}
    	$scope.execSearch();
    }
    
    $scope.nextPage = function() {
    	// update the start offset and run the search
    	$scope.startOffset += $scope.pageSize;
    	if ($scope.startOffset > $scope.numFound) {
    		$scope.startOffset -= $scope.pageSize;
    	}
    	$scope.execSearch();
    }
    
    $scope.playFile = function(filepath) {
    	// stop the audiofile if it's currently playing.
    	mrl.sendTo("audiofile", "stop");
    	// start the new song.
    	mrl.sendTo("audiofile", "playFile", filepath[0]);
    	
    //	mrl.sendTo("foobar", "play", filepath[0]);
    //	mrl.sendTo("solr", "play", filepath[0]);
    }
    
    msg.subscribe('publishResults');
    msg.subscribe(this);
    // mrl.subscribe($scope.service.name, 'publishResults', $scope.service.results);
    // $scope.panel.initDone();
                
  }]);