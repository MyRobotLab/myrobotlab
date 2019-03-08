angular.module('mrlapp.service.SolrGui', [])
  .controller('SolrGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                $log.info('SolrGuiCtrl');
                // TODO: something useful?!
                this.onMsg = function (msg) {
                    $log.info("Solr Msg ! - ");  
                    $log.info(msg);
                    if (msg.method == "onResults") {
                    	// Results!
                    	var solrResults = msg.data[0];
                    	$scope.service.solrResults = solrResults;
                    	$scope.$apply();
                    }
                };
                $scope.search = function(querystring) {
                    $log.info('SolrGuiCtrl - Search Clicked!' + querystring);
                	mrl.sendTo($scope.service.name, "search", querystring);
                };
                

                
                mrl.subscribe($scope.service.name, 'publishResults', $scope.service.results);
//                $scope.panel.initDone();
                
  }]);