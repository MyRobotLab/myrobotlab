// https://github.com/johnpapa/angular-styleguide
// http://kirkbushell.me/when-to-use-directives-controllers-or-services-in-angular/
// http://stackoverflow.com/questions/12576798/how-to-watch-service-variables
// http://stackoverflow.com/questions/19845950/angularjs-how-to-dynamically-add-html-and-bind-to-controller
// http://stackoverflow.com/questions/19446755/on-and-broadcast-in-angular
// https://www.youtube.com/watch?v=0r5QvzjjKDc
// http://odetocode.com/blogs/scott/archive/2014/05/07/using-compile-in-angular.aspx
// http://slides.wesalvaro.com/20121113/#/1/1
// http://tylermcginnis.com/angularjs-factory-vs-service-vs-provider/
// http://odetocode.com/blogs/scott/archive/2014/05/20/using-resolve-in-angularjs-routes.aspx

angular.module('mrlapp', [
    'ngRoute',
    'ng',
    'ui.bootstrap', //BootstrapUI (in Angular style)
    'oc.lazyLoad', //lazyload
    'sticky', //sticky elements
    'ngClipboard',
    'ui.ace', //funky editor
    'mrlapp.mrl', //mrl.js (/mrl.js) - the really really core
    'mrlapp.main.mrlLogger', //custom logger! (it extends the default angular one ($log))
    'mrlapp.main.mainCtrl',
    'mrlapp.main.statesvc', //very basic service for storing "statuses"
    'mrlapp.main.filter',
    'mrlapp.nav', //Navbar & Co. (/nav)
    'mrlapp.service' //Service & Co. (/service)
])
        .config(['$provide', '$routeProvider', 'mrlProvider', 'ngClipProvider',
            function ($provide, $routeProvider, mrlProvider, ngClipProvider) {

        ngClipProvider.setPath("lib/zeroclipboard/ZeroClipboard.swf");

                //set the logger up (extend the angular default one)
                $provide.decorator('$log', function ($delegate, mrlLogger) {
                    return mrlLogger($delegate);
                });

        $routeProvider.when('/main', {
            templateUrl: 'main/main.html',
            controller: 'mainCtrl',
            resolve: {
                message: function(mrl) {
                    return mrl.init();
                }
            }
//        }).when('/workpace', {
//            templateUrl: 'workpace/index.html',
        }).otherwise({
            redirectTo: '/main'
        });
    }]);
