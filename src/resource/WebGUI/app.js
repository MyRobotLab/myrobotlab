
// http://kirkbushell.me/when-to-use-directives-controllers-or-services-in-angular/
// http://stackoverflow.com/questions/12576798/how-to-watch-service-variables
// http://stackoverflow.com/questions/19845950/angularjs-how-to-dynamically-add-html-and-bind-to-controller
// http://stackoverflow.com/questions/19446755/on-and-broadcast-in-angular
// https://www.youtube.com/watch?v=0r5QvzjjKDc
// http://odetocode.com/blogs/scott/archive/2014/05/07/using-compile-in-angular.aspx
// http://slides.wesalvaro.com/20121113/#/1/1
// http://tylermcginnis.com/angularjs-factory-vs-service-vs-provider/

angular.module('mrlapp', ['ui.bootstrap', 
						'mrlapp.mrl', 
						'mrlapp.main.mainContoller', 
						'mrlapp.main.serviceDirective', 
						'ngRoute' 

						/*
														 * , 'mrlapp.wsconn',
														 * 'mrlapp.service',
														 * 'mrlapp.service.arduinogui',
														 * 'mrlapp.service.clockgui',
														 * 'mrlapp.service.webguigui',
														 * 'mrlapp.service.runtimegui'
														 */
])

.config(['$routeProvider', function($routeProvider) {
        $routeProvider.when('/main', {
            templateUrl: 'main/main.html',
            controller: 'mainContoller'
        }).when('/workpace', {
            templateUrl: 'main/workspace.html',
        //controller : 'PhoneListCtrl'
        }).when('/services', {
            templateUrl: 'main/phone-list.html',
        //controller : 'PhoneListCtrl'
        }).when('/service/:name', {
            templateUrl: 'main/phone-detail.html',
        //controller : 'PhoneDetailCtrl'
        }).otherwise({
            redirectTo: '/main'
        });
    }]);
