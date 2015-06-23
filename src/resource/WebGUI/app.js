
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
    'ui.bootstrap', 
    'mrlapp.mrl', 
    'mrlapp.main.mainCtrl', 
    'mrlapp.main.statesvc', 
    'mrlapp.nav', 
    'mrlapp.service', 
    'mrlapp.service.arduinogui', 
    'mrlapp.service.clockgui', 
    'mrlapp.service.webguigui', 
    'mrlapp.service.runtimegui', 
    'mrlapp.test.testController'
])
.config(['$routeProvider', 'mrlProvider', function($routeProvider, mrlProvider) {
        
        //mrlProvider.init();
        $routeProvider.when('/main', {
            templateUrl: 'main/main.html',
            controller: 'mainCtrl',
            resolve: {
                message: function(mrl) {
                    return mrl.init();
                }
            }
        }).when('/workpace', {
            templateUrl: 'main/workspace.html',
        }).when('/test', {
            templateUrl: 'test/test.html',
            controller: 'testController',
            resolve: {
                message: function(mrl) {
                    return mrl.init();
                }
            }
        }).otherwise({
            redirectTo: '/main'
        });
    }]);
