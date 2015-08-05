angular.module('mrlapp.service.solrgui', [])
        .controller('SolrGuiCtrl', ['$scope', '$log', 'mrl', function ($scope, $log, mrl) {
                var _self = this;
                $log.info('SolrGuiCtrl');

                this.init = function () {
                    // TODO: something useful?!
                    this.onMsg = function (msg) {
                        $log.info("Solr Msg ! - ");
                        $log.info(msg);
                        if (msg.method == "onResults") {
                            // Results!
                            var solrResults = msg.data[0];
                            $scope.data.service.solrResults = solrResults;
                            $scope.$apply();
                        }
                    };
                    $scope.data.search = function (querystring) {
                        $log.info('SolrGuiCtrl - Search Clicked!' + querystring);
                        this.send("search", querystring);
                    };

                    this.subscribe('publishResults', $scope.data.service.results);
                };

                $scope.cb.notifycontrollerisready(this);
            }]);