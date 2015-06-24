angular.module('mrlapp.service.solrgui', [])
  .controller('SolrGuiCtrl', ['$scope', 'mrl', function ($scope, mrl) {
                console.log('SolrGuiCtrl');
                // TODO: something useful?!
                $scope.methods.onMsg = function (msg) {
                    console.log("Solr Msg ! - ");  
                    console.log(msg);
                    if (msg.method == "onResults") {
                    	// Results!
                    	var solrResults = msg.data[0];
                    	$scope.data.solrResults = solrResults;
                    	$scope.$apply();
                    }
                    
                    
                };
                
                $scope.search = function(querystring) {
                    console.log('SolrGuiCtrl - Search Clicked!');
                	mrl.sendTo($scope.data.name, "search", querystring);
                	
                };
                
                mrl.subscribe($scope.data.name, 'publishResults', $scope.data.results);
                
                $scope.fw.initDone();
                
                
                
  }]);