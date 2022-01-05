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
'ui.bootstrap', //BootstrapUI (in Angular style)
'oc.lazyLoad', //lazyload
'sticky', //sticky elements
'checklist-model', // checklists
'peer', 'color.picker', 'angular-intro', // intro
'angularScreenfull', // screenfull
'angular-clipboard', 'ngFlash', 'ui.ace', //funky editor
'timer', 'luegg.directives', // scrollglue
'mrlapp.mrl', //mrl.js (/mrl.js) - core communication and service registry
//'mrlapp.main.mainCtrl', 
'mrlapp.main.statusSvc', //very basic service for storing "statuses"
'ModalController', 'modalService', 'chart.js', 'mrlapp.main.noWorkySvc', //send a noWorky !
'mrlapp.widget.startCtrl', 'mrlapp.nav', //Navbar & Co. (/nav)
'mrlapp.service', //Service & Co. (/service)
'angularTreeview', // any server filesystem browsing
'ui.toggle', // toggle buttons
'ui.select', // select option with images
'mrlapp.utils'//general, helful tools, directives, services, ...
]).config(['$provide', '$stateProvider', '$urlRouterProvider', 'mrlProvider', function($provide, $stateProvider, $urlRouterProvider, mrlProvider) {
    console.info('app.js - starting')
    console.info('app.js - config defining routes')

    $urlRouterProvider.otherwise("/service/intro");
    // default redirect

    $stateProvider.state('tabs2', {
        url: "/service/:servicename",
        views: {
            '': {
                templateUrl: 'main/main.html'
            },
            'navbar@tabs2': {
                templateUrl: 'nav/nav.html',
                controller: 'navCtrl'
            },
            'content@tabs2': {
                templateUrl: 'views/tabsView.html',
                controller: 'tabsViewCtrl'
            }
        },
        resolve: {
            test: function($stateParams, mrl) {
                console.info('tabs2.main state in router')
            }
        }
    })
}
])
