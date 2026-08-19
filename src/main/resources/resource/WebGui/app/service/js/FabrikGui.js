angular.module('mrlapp.service.FabrikGui', [])
.controller('FabrikGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
        $log.info('FabrikGui');

        var _self = this;
        var msg = this.msg;
        var SLIDER_SPAN_M = 2;
        var SLIDER_MIN_Y_M = 0;
        var SLIDER_MAX_Y_M = 4;

        $scope.service = mrl.getService($scope.service.name);
        $scope.positions = [];
        $scope.angles = {};
        $scope.worldPosition = $scope.service.worldPosition || null;
        $scope.lastSolveError = $scope.service.lastSolveError;
        $scope.lastSolveIterations = $scope.service.lastSolveIterations;
        $scope.originX = 0;
        $scope.originY = 0;
        $scope.originZ = 0;
        // World-meter slider box: X/Z ±2 m, Y 0–4 m (VinMoov is above the floor)
        $scope.sliderMinX = -SLIDER_SPAN_M;
        $scope.sliderMaxX = SLIDER_SPAN_M;
        $scope.sliderMinY = SLIDER_MIN_Y_M;
        $scope.sliderMaxY = SLIDER_MAX_Y_M;
        $scope.sliderMinZ = -SLIDER_SPAN_M;
        $scope.sliderMaxZ = SLIDER_SPAN_M;
        $scope.x = null;
        $scope.y = null;
        $scope.z = null;
        applyOrigin($scope.service);
        // Seed the MoveTo boxes once. After that they are the user's goal and
        // must not be overwritten by live palm / onState / onMoveTo.
        seedGoal($scope.service.ikGoal || $scope.worldPosition);

        this.updateState = function (service) {
            $scope.service = service;
            applyOrigin(service);
            $scope.lastSolveError = service.lastSolveError;
            $scope.lastSolveIterations = service.lastSolveIterations;
            if (service && service.worldPosition) {
                $scope.worldPosition = service.worldPosition;
            }
            seedGoal(service && (service.ikGoal || service.worldPosition));
        };

        function applyOrigin(service) {
            var cfg = service && service.config;
            var ox = 0;
            var oy = 0;
            var oz = 0;
            if (cfg) {
                ox = cfg.originX || 0;
                oy = cfg.originY || 0;
                oz = cfg.originZ || 0;
            }
            $scope.originX = ox;
            $scope.originY = oy;
            $scope.originZ = oz;
            $scope.sliderMinX = -SLIDER_SPAN_M;
            $scope.sliderMaxX = SLIDER_SPAN_M;
            $scope.sliderMinY = SLIDER_MIN_Y_M;
            $scope.sliderMaxY = SLIDER_MAX_Y_M;
            $scope.sliderMinZ = -SLIDER_SPAN_M;
            $scope.sliderMaxZ = SLIDER_SPAN_M;
        }

        function seedGoal(p) {
            if ($scope.x !== null && $scope.y !== null && $scope.z !== null) {
                return;
            }
            applyGoal(p);
        }

        function applyGoal(p) {
            if (!p) {
                return;
            }
            $scope.x = roundCoord(p.x);
            $scope.y = roundCoord(p.y);
            $scope.z = roundCoord(p.z);
        }

        function roundCoord(v) {
            if (v === null || v === undefined) {
                return v;
            }
            return Math.round(v * 10000) / 10000;
        }

        function parseCoord(v) {
            if (v === null || v === undefined || v === '') {
                return null;
            }
            var n = parseFloat(v);
            return isNaN(n) ? null : n;
        }

        this.onMsg = function(inMsg) {
            switch (inMsg.method) {
                case 'onState':
                    _self.updateState(inMsg.data[0]);
                    $scope.$apply();
                    break;
                case 'onWorldPosition':
                    // Live FK palm — do not overwrite the MoveTo boxes
                    $scope.worldPosition = inMsg.data[0];
                    $scope.$apply();
                    break;
                case 'onIkGoal':
                    // Green-marker goal. Do not copy into the MoveTo boxes —
                    // moveTo publishes this and would snap the controls back.
                    $scope.$apply();
                    break;
                case 'onMoveTo':
                    // Reached palm, not the goal the user is editing
                    if (inMsg.data && inMsg.data[0]) {
                        $scope.worldPosition = inMsg.data[0];
                    }
                    $scope.$apply();
                    break;
                case 'onCenterAllJoints':
                case 'onComputePositionFromServos':
                case 'onCalibrateFromSimulator':
                    if (inMsg.data && inMsg.data[0]) {
                        $scope.worldPosition = inMsg.data[0];
                        applyGoal(inMsg.data[0]);
                    }
                    $scope.$apply();
                    break;
                case 'onJointPositions':
                    $scope.positions = inMsg.data[0];
                    $scope.$apply();
                    break;
                case 'onJointAngles':
                    $scope.angles = inMsg.data[0];
                    $scope.$apply();
                    break;
                default:
                    $log.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method);
                    break;
            }
        };

        $scope.centerAllJoints = function() {
            msg.send('centerAllJoints');
        };

        $scope.computePositionFromServos = function() {
            msg.send('computePositionFromServos');
        };

        $scope.calibrateFromSimulator = function() {
            msg.send('calibrateFromSimulator');
        };

        $scope.moveTo = function(x, y, z) {
            var nx = parseCoord(x);
            var ny = parseCoord(y);
            var nz = parseCoord(z);
            if (nx === null || ny === null || nz === null) {
                $log.warn('moveTo needs all three world coordinates, got', x, y, z);
                return;
            }
            nx = roundCoord(nx);
            ny = roundCoord(ny);
            nz = roundCoord(nz);
            // Keep the boxes on the value the user just set. roundCoord can
            // stringify-vs-number the range input; write back the numbers.
            $scope.x = nx;
            $scope.y = ny;
            $scope.z = nz;
            $log.info('FABRIK moveTo world', nx, ny, nz);
            msg.send('moveTo', nx, ny, nz);
        };

        msg.subscribe('publishWorldPosition');
        msg.subscribe('publishIkGoal');
        msg.subscribe('publishJointPositions');
        msg.subscribe('publishJointAngles');
        msg.subscribe('moveTo');
        msg.subscribe('centerAllJoints');
        msg.subscribe('computePositionFromServos');
        msg.subscribe('calibrateFromSimulator');
        msg.subscribe(this);
    }]);
