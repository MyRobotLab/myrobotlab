//jshint strict: false
module.exports = function (config) {
    config.set({
        basePath: './app',
        files: [
			'bower_components/atmosphere.js/atmosphere.min.js',
			'bower_components/jquery/dist/jquery.min.js',
			'bower_components/jquery-ui/jquery-ui.min.js',
			'bower_components/bootstrap/dist/js/bootstrap.min.js',
			'bower_components/angular/angular.min.js',
			'bower_components/angular-animate/angular-animate.min.js',
			'bower_components/angular-ui-router/release/angular-ui-router.min.js',
			'bower_components/ui-router-extras/release/ct-ui-router-extras.min.js',
			'bower_components/angular-bootstrap/ui-bootstrap-tpls.js',
			'bower_components/oclazyload/dist/ocLazyLoad.min.js',
			'lib/sticky.min.js',
			'lib/scrollglue-2.0.6.js',
			'bower_components/angularjs-nvd3-directives/dist/angularjs-nvd3-directives.min.js',
			'bower_components/d3/d3.min.js',
			'bower_components/nvd3/build/nv.d3.min.js',
			'bower_components/ace-builds/src-min-noconflict/ace.js',
			'bower_components/angular-ui-ace/ui-ace.js',
			'lib/angular-clipboard-1.3.0/angular-clipboard.js',
			'lib/intro.js-2.1.0/intro.min.js',
			'lib/intro.js-2.1.0/angular-intro.min.js',
			'lib/angularjs-slider-2.4.0/rzslider.min.js',
			'app.js',
			'mrl.js',
			'main/mainCtrl.js',
			'main/loadingCtrl.js',
			'main/statusSvc.js',
			'main/noWorkySvc.js',
			'views/viewsModule.js',
			'views/mainViewCtrl.js',
			'views/tabsViewCtrl.js',
			'views/serviceViewCtrl.js',
			'widget/startCtrl.js',
			'nav/navModule.js',
			'nav/navCtrl.js',
			'nav/navDirective.js',
			'nav/aboutCtrl.js',
			'nav/noWorkyCtrl.js',
			'nav/shutdownCtrl.js',
			'service/serviceModule.js',
			'service/serviceSvc.js',
			'service/serviceCtrl.js',
			'service/serviceFullCtrl.js',
			'service/serviceDirective.js',
			'service/serviceBodyDirective.js',
			'service/serviceCtrlDirective.js',
			'service/dragDirective.js',
			'service/resizeDirective.js',
			'utils/utilsModule.js',
			'utils/filter.js',
			'utils/parseHtmlDirective.js',
			'widget/oscope.js',
			'widget/pressEnter.js',
			'lib/timer-1.3.3/angular-timer.min.js',
			'lib/timer-1.3.3/humanize-duration.js',
			'lib/timer-1.3.3/moment.min.js',
			'lib/tinycolor-1.3.0/tinycolor-min.js',
			'lib/tinygradient-0.3.0/tinygradient.min.js',
			'lib/checklist-model-20160510/checklist-model.js',
			'bower_components/three.js/build/three.min.js',
			'bower_components/three.js/examples/js/loaders/AssimpJSONLoader.js',
			'widget/three.js',
			'mrl_test.js',
        ],
        autoWatch: true,
        frameworks: ['jasmine'],
        browsers: ['Chrome', "Firefox"],
        plugins: [
            'karma-chrome-launcher',
            'karma-firefox-launcher',
            'karma-jasmine',
            'karma-junit-reporter',
            'karma-coverage'
        ],
        junitReporter: {
            outputFile: 'test_out/unit.xml',
            suite: 'unit'
        },
        reporters: ['progress', 'coverage'],
        preprocessors: {
            // source files, that you wanna generate coverage for
            // do not include tests or libraries
            // (these files will be instrumented by Istanbul)
			'{*.js,!(lib|bower_components)/**/*.js}': ['coverage']
        },
        // optionally, configure the reporter
        coverageReporter: {
            // specify a common output directory
            dir: '../build/reports/coverage',
            reporters: [
                // reporters not supporting the `file` property
                {type: 'html', subdir: 'report-html'},
                {type: 'lcov', subdir: 'report-lcov'},
                // reporters supporting the `file` property, use `subdir` to directly
                // output them in the `dir` directory
                {type: 'cobertura', subdir: '.', file: 'cobertura.txt'},
                {type: 'lcovonly', subdir: '.', file: 'report-lcovonly.txt'},
                {type: 'teamcity', subdir: '.', file: 'teamcity.txt'},
                {type: 'text', subdir: '.', file: 'text.txt'},
                {type: 'text-summary', subdir: '.', file: 'text-summary.txt'},
            ]
        }
    });
};
