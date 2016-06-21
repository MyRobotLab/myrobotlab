angular.module('mrlapp.nav')
        .directive('navDirective', [function () {
                return {
                    scope: {
                        //"=" -> binding to items in parent-scope specified by attribute
                        //"@" -> using passed attribute
                    },
                    templateUrl: 'nav/nav.html',
                    controller: 'navCtrl'
                };
            }]);
