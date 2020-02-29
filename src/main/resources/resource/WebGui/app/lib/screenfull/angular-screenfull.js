/* global angular */
(function() {
    angular.module('angularScreenfull', []);
})();

/* global angular, screenfull */
(function() {
    'use strict';

    angular
        .module('angularScreenfull')
        .directive('ngsfFullscreen', ngsfFullscreenDirective);

    /**
     * @ngdoc directive
     * @name angularScreenfull.directive:ngsfFullscreen
     * @restrict A
     *
     * @description
     * Marks the element that is going to be fullscreen
     *
     * @param {string=}  ngsfFullscreen  An optional expression to store the fullscreen controller
     *
     * @example
   <example  module="myApp">
     <file name="app.js">
        angular.module('myApp', ['angularScreenfull']);
     </file>
     <file name="index.html">
        <div ngsf-fullscreen>
            <p>This is a fullscreen element</p>
            <button ngsf-toggle-fullscreen>Toggle fullscreen</button>
        </div>
     </file>
   </example>
     */

    ngsfFullscreenDirective.$inject = ['$parse'];
    function ngsfFullscreenDirective ($parse) {
        return {
            restrict: 'A',
            require: 'ngsfFullscreen',
            controller: NgsfFullscreenController,
            link: link
        };

        function link (scope, elm, attrs, ctrl) {
            // If the directive has a value, add the controller to the scope under that name
            if (attrs.ngsfFullscreen && attrs.ngsfFullscreen !== '') {
                var p = $parse(attrs.ngsfFullscreen);
                p.assign(scope, ctrl);
            }
        }
    }

    NgsfFullscreenController.$inject = ['$scope', '$document', '$element', '$animate'];
    function NgsfFullscreenController ($scope, $document, $elm, $animate) {
        var ctrl = this;

        ctrl.onFullscreenChange = onFullscreenChange;
        ctrl.onFullscreenChangeComplete = onFullscreenChangeComplete;
        ctrl.requestFullscreen = requestFullscreen;
        ctrl.removeFullscreen = removeFullscreen;
        ctrl.toggleFullscreen = toggleFullscreen;
        ctrl.isFullscreen = isFullscreen;
        ctrl.fullscreenEnabled = fullscreenEnabled;

        function subscribeToEvents () {
            var fullscreenchange = function () {
                $animate[ctrl.isFullscreen() ? 'addClass' : 'removeClass']($elm, 'fullscreen').then(function(){
                  $scope.$emit('fullscreenchangecomplete');
                });
                // TODO: document using ngdoc
                $scope.$emit('fullscreenchange');
                $scope.$apply();
            };

            $document[0].addEventListener(screenfull.raw.fullscreenchange, fullscreenchange);
            $scope.$on('$destroy', function() {
                $document[0].removeEventListener(screenfull.raw.fullscreenchange, fullscreenchange);
            });
        }
        if (ctrl.fullscreenEnabled()) {
            subscribeToEvents();
        }

        ////////////////////////////////////////

        function onFullscreenChange (handler) {
            return $scope.$on('fullscreenchange', handler);
        }

        function onFullscreenChangeComplete (handler) {
            return $scope.$on('fullscreenchangecomplete', handler);
        }

        function requestFullscreen () {
            if (ctrl.fullscreenEnabled()) {
                screenfull.request($elm[0]);
                $scope.$emit('fullscreenEnabled');
                return true;
            }
            return false;
        }

        function removeFullscreen () {
            if (ctrl.fullscreenEnabled()) {
                if (ctrl.isFullscreen()) {
                    ctrl.toggleFullscreen();
                }
            }
        }

        function toggleFullscreen () {
            if (ctrl.fullscreenEnabled()) {
                var isFullscreen = screenfull.isFullscreen;
                screenfull.toggle($elm[0]);
                if (isFullscreen) {
                    $scope.$emit('fullscreenDisabled');
                } else {
                    $scope.$emit('fullscreenEnabled');
                }
                return true;
            }
            return false;
        }

        function isFullscreen () {
            if (ctrl.fullscreenEnabled()) {
                return screenfull.isFullscreen;
            }
            return false;
        }

        function fullscreenEnabled () {
            if (typeof screenfull !== 'undefined') {
                return screenfull.isEnabled;
            }
            return false;
        }
    }
})();


/* global angular */
(function() {
    'use strict';

    angular
        .module('angularScreenfull')
        .directive('showIfFullscreenEnabled', showIfFullscreenEnabledDirective);

    /**
     * @ngdoc directive
     * @name angularScreenfull.directive:showIfFullscreenEnabled
     * @restrict A
     *
     * @description
     * Shows or hides the element (using ng-hide) if the browser has fullscreen
     * capabilities.
     *
     */

    showIfFullscreenEnabledDirective.$inject = ['$animate'];

    function showIfFullscreenEnabledDirective ($animate) {
        // Directive definition
        return {
            restrict: 'A',
            require: '^ngsfFullscreen',
            link: link
        };

        function link (scope, elm, attrs, fullScreenCtrl) {
            if (fullScreenCtrl.fullscreenEnabled()) {
                $animate.removeClass(elm, 'ng-hide');
            } else {
                $animate.addClass(elm, 'ng-hide');
            }
        }
    }
})();

/* global angular */
(function() {
    'use strict';

    angular
        .module('angularScreenfull')
        .directive('showIfFullscreen', showIfFullscreenDirective);

    /**
     * @ngdoc directive
     * @name angularScreenfull.directive:showIfFullscreen
     * @restrict A
     *
     * @description
     * Shows or hides the element (using ng-hide) if the closest
     * parent that has the ngsf-fullscreen directive is in fullscreen mode.
     *
     * By default the element shows itself if its fullscreen or hides otherwise, but you can
     * change this behaviour by passing false to the directive
     *
     * @param {boolean=}  showIfFullscreen   If false it inverts the show/hide behaviour. Defaults to true.
     *
     */

    showIfFullscreenDirective.$inject = ['$animate'];

    function showIfFullscreenDirective ($animate) {
        // Directive definition
        return {
            restrict: 'A',
            require: '^ngsfFullscreen',
            link: link
        };

        function link (scope, elm, attrs, fullScreenCtrl) {
            var hideOrShow = function () {

                var show = fullScreenCtrl.isFullscreen();
                if (attrs.showIfFullscreen === 'false' || attrs.showIfFullscreen === false) {
                    show = !show;
                }

                if (show) {
                    $animate.removeClass(elm, 'ng-hide');
                } else {
                    $animate.addClass(elm, 'ng-hide');
                }
            };
            hideOrShow();
            var unwatch = fullScreenCtrl.onFullscreenChange(hideOrShow);
            scope.$on('$destroy', unwatch);
        }
    }
})();

/* global angular */
(function() {
    'use strict';

    angular
        .module('angularScreenfull')
        .directive('ngsfToggleFullscreen', ngsfToggleFullscreenDirective);

    /**
     * @ngdoc directive
     * @name angularScreenfull.directive:ngsfToggleFullscreen
     * @restrict A
     *
     * @description
     * Adds a click handler to the element that toggles the nearest ngsf-fullscreen element
     *
     */

    function ngsfToggleFullscreenDirective () {
        // Directive definition
        return {
            restrict: 'A',
            require: '^ngsfFullscreen',
            link: link
        };

        function link (scope, elm, attr, fullScreenCtrl) {
            elm.on('click', function() {
                fullScreenCtrl.toggleFullscreen();
            });
        }
    }
})();
