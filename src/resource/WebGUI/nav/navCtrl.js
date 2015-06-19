angular.module('mrlapp.nav', [
    'mrlapp.mrl',
    'mrlapp.main.statesvc'
])

        .controller('navCtrl', ['$scope', '$location', '$anchorScroll', 'StateSvc',
            function ($scope, $location, $anchorScroll, StateSvc) {
                $scope.statuslist = StateSvc.getStatuses();

                StateSvc.addStatus('And this is my status history!');
                StateSvc.addStatus('And this is my status history!');
                StateSvc.addStatus('And this is my status history!');
                StateSvc.addStatus('I am going to be the new WebUI for MyRobotLab!');

                $scope.about = function () {
                    console.log('about');
                };

                $scope.help = function () {
                    console.log('help');
                };

                //TODO: find a way to get all Services - probably something like mrl.getAllServices()
                $scope.searchServices = [];
                
                $scope.searchOnSelect = function (item, model, label) {
                    console.log('searchOnSelect');
                    //scroll to selected service
                    $location.hash(item.name);
                    $anchorScroll();
                };
            }])

        .filter('reverse', function () {
            return function (items) {
                return items.slice().reverse();
            };
        });
