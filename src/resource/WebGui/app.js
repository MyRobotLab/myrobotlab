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
angular.module('mrlapp', ['ng', 'ngAnimate', //Angular Animate
'ui.router', //Angular UI Router - Yeah!
'ct.ui.router.extras.previous', //Angular UI Router Extras _ PreviousState - Yeah!Yeah!
'ui.bootstrap', //BootstrapUI (in Angular style)
'oc.lazyLoad', //lazyload
'sticky', //sticky elements
'checklist-model', // checklists
'angular-intro', // intro
//'ngclipboard',
'angular-clipboard', 'rzModule', 'ngFlash', //'charts',
'nvd3ChartDirectives', 'ui.ace', //funky editor
'timer', 'luegg.directives', // scrollglue
'mrlapp.mrl', //mrl.js (/mrl.js) - core communication and service registry
'mrlapp.main.mainCtrl', 'mrlapp.main.statusSvc', //very basic service for storing "statuses"
'mrlapp.main.noWorkySvc', //send a noWorky !
'mrlapp.views', //mainView, tabsView, serviceView, ...
'mrlapp.widget.startCtrl', 'mrlapp.nav', //Navbar & Co. (/nav)
'mrlapp.service', //Service & Co. (/service)
'mrlapp.utils'//general, helful tools, directives, services, ...
]).config(['$provide', '$stateProvider', '$urlRouterProvider', 'mrlProvider', function($provide, $stateProvider, $urlRouterProvider, mrlProvider) {
    console.log('app.js');
    $urlRouterProvider.otherwise("/main");
    $stateProvider.state('loading', {
        url: "/loading",
        templateUrl: "main/loading.html",
        controller: 'loadingCtrl',
        resolve: {
            test: function($stateParams, mrl) {
                console.log('calling mrl.init() from router')
                return mrl.init();
            }
        }
    }).state('main', {
        url: "/main",
        template: "<div ui-view></div>",
        controller: 'mainCtrl',
        resolve: {
            test: function($stateParams, mrl) {
                console.log('calling mrl.init() from router')
                return mrl.init();
            }
        }
    }).state('main.main', {
        views: {
            '': {
                templateUrl: 'main/main.html'
            },
            'navbar@main.main': {
                templateUrl: 'nav/nav.html',
                controller: 'navCtrl'
            },
            'content@main.main': {
                templateUrl: 'views/mainView.html',
                controller: 'mainViewCtrl'
            }
        },
        resolve: {
            test: function($stateParams, mrl) {
                console.log('calling mrl.init() from router')
                return mrl.init();
            }
        }
    }).state('tabs', {
        url: "/tabs",
        template: "<div ui-view></div>",
        controller: 'mainCtrl',
        resolve: {
            test: function($stateParams, mrl) {
                console.log('calling mrl.init() from router')
                return mrl.init();
            }
        }
    }).state('tabs.main', {
        views: {
            '': {
                templateUrl: 'main/main.html'
            },
            'navbar@tabs.main': {
                templateUrl: 'nav/nav.html',
                controller: 'navCtrl'
            },
            'content@tabs.main': {
                templateUrl: 'views/tabsView.html',
                controller: 'tabsViewCtrl'
            }
        },
        resolve: {
            test: function($stateParams, mrl) {
                console.log('calling mrl.init() from router')
                return mrl.init();
            }
        }
    }).state('serviceView', {
        url: '/service/:servicename',
        template: "<div ui-view></div>",
        controller: 'mainCtrl',
        resolve: {
            test: function($stateParams, mrl) {
                console.log('calling mrl.init() from router')
                return mrl.init();
            }
        }
    }).state('serviceView.main', {
        views: {
            '': {
                templateUrl: 'main/main.html'
            },
            'navbar@serviceView.main': {},
            'content@serviceView.main': {
                templateUrl: 'views/serviceView.html',
                controller: 'serviceViewCtrl',
                resolve: {
                    test: function($stateParams, mrl) {
                        console.log('calling mrl.init() from router')
                        return mrl.init();
                    }
                }
            }
        }
    });
}
]);
