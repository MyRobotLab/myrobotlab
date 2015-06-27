angular.module('mrlapp.service.solrgui', [])
  .controller('SolrGuiCtrl', ['$scope', 'mrl', function ($scope, mrl) {
                console.log('SolrGuiCtrl');
                // TODO: something useful?!
                $scope.service.onMsg = function (msg) {
                    console.log("Solr Msg ! - ");  
                    console.log(msg);
                    if (msg.method == "onResults") {
                    	// Results!
                    	var solrResults = msg.service[0];
                    	$scope.service.solrResults = solrResults;
                    	$scope.$apply();
                    }
                    
                    
                };
                
                $scope.search = function(querystring) {
                    console.log('SolrGuiCtrl - Search Clicked!');
                	mrl.sendTo($scope.service.name, "search", querystring);
                	
                };
                
                mrl.subscribe($scope.service.name, 'publishResults', $scope.service.results);
                
                $scope.gui.initDone();
                
                
                
  }]);