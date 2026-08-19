angular.module('mrlapp.service.InverseKinematics3DGui', [])
.controller('InverseKinematics3DGuiCtrl', ['$scope', '$log', 'mrl', function($scope, $log, mrl) {
        $log.info('InverseKinematics3D');
        
        var _self = this;
        var msg = this.msg;
        // init scope variables
        
        // get latest copy of a services
        $scope.service = mrl.getService($scope.service.name);
        $scope.interval = $scope.service.interval;
        $scope.positions = '';
        $scope.angles = '';
        $scope.tracking = '';
        $scope.worldPosition = $scope.service.worldPosition || null;
        $scope.verifyResult = null;
        $scope.originX = 0;
        $scope.originY = 0;
        $scope.originZ = 0;
        var SLIDER_SPAN_M = 2;
        var SLIDER_MIN_Y_M = 0;
        var SLIDER_MAX_Y_M = 4;
        // Same world-meter box as FabrikGui: X/Z ±2 m, Y 0–4 m
        $scope.sliderMinX = -SLIDER_SPAN_M;
        $scope.sliderMaxX = SLIDER_SPAN_M;
        $scope.sliderMinY = SLIDER_MIN_Y_M;
        $scope.sliderMaxY = SLIDER_MAX_Y_M;
        $scope.sliderMinZ = -SLIDER_SPAN_M;
        $scope.sliderMaxZ = SLIDER_SPAN_M;
        $scope.x = null;
        $scope.y = null;
        $scope.z = null;
        applyWorldPosition($scope.worldPosition);
        applyOrigin($scope.service);
        // GOOD TEMPLATE TO FOLLOW
        this.updateState = function (service) {
            $scope.service = service;
            applyOrigin(service);
            if (service && service.worldPosition) {
                $scope.worldPosition = service.worldPosition;
                applyWorldPosition(service.worldPosition);
            }
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

        function applyWorldPosition(p) {
            if (!p) {
                return;
            }
            $scope.worldPosition = p;
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

        // Rendering area helper functions
        function buildAxis( src, dst, colorHex, dashed ) {
	        var geom = new THREE.Geometry(),
            mat; 
            if(dashed) {
                mat = new THREE.LineDashedMaterial({ linewidth: 3, color: colorHex, dashSize: 3, gapSize: 3 });
            } else {
                mat = new THREE.LineBasicMaterial({ linewidth: 3, color: colorHex });
            }
            geom.vertices.push( src.clone() );
            geom.vertices.push( dst.clone() );
            geom.computeLineDistances(); // This one is SUPER important, otherwise dashed lines will appear as simple plain lines
            var axis = new THREE.Line( geom, mat, THREE.LinePieces );
            return axis;
        };
        
		//ref: http://soledadpenades.com/articles/three-js-tutorials/drawing-the-coordinate-axes/
		function buildAxes( length ) {
			    var xColor = 0x990000;
			    var yColor = 0x009900;
			    var zColor = 0x000099;
		        var axes = new THREE.Object3D();
		        axes.add( buildAxis( new THREE.Vector3( 0, 0, 0 ), new THREE.Vector3( length, 0, 0 ), xColor, false ) ); // +X
		        axes.add( buildAxis( new THREE.Vector3( 0, 0, 0 ), new THREE.Vector3( -length, 0, 0 ), xColor, true) ); // -X
		        axes.add( buildAxis( new THREE.Vector3( 0, 0, 0 ), new THREE.Vector3( 0, length, 0 ), yColor, false ) ); // +Y
		        axes.add( buildAxis( new THREE.Vector3( 0, 0, 0 ), new THREE.Vector3( 0, -length, 0 ), yColor, true ) ); // -Y
		        axes.add( buildAxis( new THREE.Vector3( 0, 0, 0 ), new THREE.Vector3( 0, 0, length ), zColor, false ) ); // +Z
		        axes.add( buildAxis( new THREE.Vector3( 0, 0, 0 ), new THREE.Vector3( 0, 0, -length ), zColor, true ) ); // -Z
		        return axes;
		};
        
		
		// document.body.appendChild( container );
		//  TODO: how do we tell it to use the container?
		var renderer = new THREE.WebGLRenderer();
		renderer.setSize( 400, 400 );
		// add the x,y,z axis lines to the scene.
		var axis = buildAxes( 1 );
		//var camera = new THREE.PerspectiveCamera( 75, window.innerWidth/window.innerHeight, 0.1, 1000 );
		var camera = new THREE.PerspectiveCamera( 75, 1, 0.01, 50 );
		camera.position.x = 1.5;
		camera.position.y = 2.0;
		camera.position.z = 3.0;
		camera.lookAt(new THREE.Vector3(0.2, 1.5, 0));
		bindCanvasOrbit(renderer, camera);

		function bindCanvasOrbit(webglRenderer, cam) {
			var el = webglRenderer.domElement;
			if (el._ikOrbitBound) {
				return;
			}
			el._ikOrbitBound = true;
			el.style.cursor = 'grab';
			el.title = 'Left-drag to orbit, wheel to zoom';

			var lookAt = new THREE.Vector3(0.2, 1.5, 0);
			var dragging = false;
			var lastX = 0;
			var lastY = 0;
			var radius = cam.position.length();
			var theta = Math.atan2(cam.position.x, cam.position.z);
			var phi = Math.acos(Math.max(-1, Math.min(1, cam.position.y / Math.max(radius, 1e-6))));

			function applyOrbit() {
				phi = Math.max(0.05, Math.min(Math.PI - 0.05, phi));
				radius = Math.max(0.3, Math.min(12, radius));
				cam.position.x = radius * Math.sin(phi) * Math.sin(theta);
				cam.position.y = radius * Math.cos(phi);
				cam.position.z = radius * Math.sin(phi) * Math.cos(theta);
				cam.lookAt(lookAt);
			}

			el.addEventListener('mousedown', function(e) {
				if (e.button !== 0) {
					return;
				}
				dragging = true;
				lastX = e.clientX;
				lastY = e.clientY;
				el.style.cursor = 'grabbing';
				e.preventDefault();
			});
			window.addEventListener('mouseup', function() {
				dragging = false;
				el.style.cursor = 'grab';
			});
			window.addEventListener('mousemove', function(e) {
				if (!dragging) {
					return;
				}
				theta -= (e.clientX - lastX) * 0.01;
				phi -= (e.clientY - lastY) * 0.01;
				lastX = e.clientX;
				lastY = e.clientY;
				applyOrbit();
			});
			el.addEventListener('wheel', function(e) {
				e.preventDefault();
				radius *= e.deltaY > 0 ? 1.1 : 0.9;
				applyOrbit();
			});
		}
        
		
		
		
        this.onMsg = function(msg) {
            $log.info("On Message IK3D!");
            $log.info(msg);
            switch (msg.method) {
                case 'onState':
                    _self.updateState(msg.data[0]);
                    $scope.$apply();
                    break;
                case 'onWorldPosition':
                    // Live FK palm — do not overwrite the MoveTo boxes (those are the goal)
                    $scope.worldPosition = msg.data[0];
                    $scope.$apply();
                    break;
                case 'onIkGoal':
                    if (msg.data && msg.data[0]) {
                        $scope.x = roundCoord(msg.data[0].x);
                        $scope.y = roundCoord(msg.data[0].y);
                        $scope.z = roundCoord(msg.data[0].z);
                    }
                    $scope.$apply();
                    break;
                case 'onMoveTo':
                case 'onComputePositionFromServos':
                case 'onCalibrateFromSimulator':
                    if (msg.data && msg.data[0]) {
                        $scope.worldPosition = msg.data[0];
                    }
                    $scope.$apply();
                    break;
                case 'onVerifyAgainstSimulator':
                    var worst = msg.data ? msg.data[0] : null;
                    if (worst === null || worst === undefined || isNaN(worst)) {
                        $scope.verifyResult = 'could not run - is the simulator started?';
                    } else {
                        $scope.verifyResult = 'worst disagreement ' + (Math.round(worst * 10000) / 10) + ' mm'
                            + (worst > 0.005 ? ' - recalibrate, the model does not track the arm' : ' - model tracks the arm');
                    }
                    $scope.$apply();
                    break;
                case 'onJointPositions':
                	//$log.info("On Joint Positions..");
                    $scope.positions = msg.data[0];
                    $scope.$apply();
                    // Our Javascript will go here.
            		// ref: http://soledadpenades.com/articles/three-js-tutorials/drawing-the-coordinate-axes/
                    var container = document.getElementById( 'canvas' );
            		// $log.info("CANVAS CONTAINER: " + container);
            		if (container.hasChildNodes()) {
            			container.removeChild( container.childNodes[0]);
            			container.appendChild( renderer.domElement );	
            		} else {
            			container.appendChild( renderer.domElement );
            		}

            		// Initialize the scene.
            		var scene = new THREE.Scene();
            		scene.add( axis );
            		
            		// scene.children={};
            		//container.appendChild( renderer.domElement );
            		// var renderer = new THREE.WebGLRenderer();
            		//renderer.setSize( window.innerWidth, window.innerHeight );
            		// renderer.setSize( 400,400);
            		// startpoint/stoppoint for each link as array  [ [ x1,y1,z1 ] , [ x2,y2,z2 ] ] stop x,y,z  points.
            		// This is the start/stop positions of all the joints
            		 //console.log($scope.positions);
            		
            		 var linkPoints = [];
            		 for (i = 0 ; i < $scope.positions.length-1; i++) {
            			 //console.log("Push a point!");
            			 //console.log(i);
            			 //console.log($scope.positions.length);
            			 //console.log("Push a point 2!");
            			 var startStopPoint = [];
            			 startStopPoint.push($scope.positions[i]);
            			 startStopPoint.push($scope.positions[i+1]);
            			 linkPoints.push(startStopPoint);
            		 };
            		
            		//var linkPoints = [
            		//  [[0,0,0], [10,10,10]],
            		//  [[10,10,10], [15,1,0]]
            		//];
            		var numLinks = linkPoints.length;
            		var colorHex = [0x0000ff, 0x00ff00];	
            		var numColors = colorHex.length;
            		for (i = 0; i < numLinks; i++) {
            			// start and end point for this link.
            			var startPoint = linkPoints[i][0];
            			var stopPoint = linkPoints[i][1];
            			// the color to render for this link 
            			var color = colorHex[i%numColors];
            			// create 2 vector objects representing the start/stop points for the line/
            			var start = new THREE.Vector3( startPoint[0], startPoint[1], startPoint[2] );
            			var stop = new THREE.Vector3( stopPoint[0], stopPoint[1], stopPoint[2] );
            			var lineGeom = new THREE.Geometry();
            		    var lineMat = new THREE.LineBasicMaterial({ linewidth: 5, color: color });
            		    // push the points for the start/stop of the line.
            		    lineGeom.vertices.push( start );
            		    lineGeom.vertices.push( stop );
            		    // create a segment 
            		    var segment = new THREE.Line( lineGeom, lineMat, THREE.LinePieces );
            		    // add the line segment to the scene.
            			scene.add( segment );
            		}; 

            		var render = function () {
            			requestAnimationFrame( render );
            			renderer.render(scene, camera);
            		};

            		// finally render the scene.
            		render();
            		break;
                case 'onJointAngles':
                case 'onJointAngle':
                	$log.info("On Joint Angles..");
                	$scope.angles = msg.data[0];
                	$scope.$apply();
                	break;
                case 'onTracking':
                	$log.info("On Tracking called.");
                	$scope.tracking = msg.data[0];
                	$scope.$apply();
                	break;
                default:
                    $log.error("ERROR - unhandled method " + $scope.name + " " + msg.method);
                    break;
            };
        };

        
        $scope.centerAllJoints = function() {
            msg.send('centerAllJoints');
        };

        $scope.computePositionFromServos = function() {
            msg.send('computePositionFromServos');
        };

        $scope.calibrateFromSimulator = function() {
            $scope.verifyResult = null;
            msg.send('calibrateFromSimulator');
        };

        $scope.verifyAgainstSimulator = function() {
            $scope.verifyResult = 'sweeping joints...';
            msg.send('verifyAgainstSimulator');
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
            $log.info('moveTo world', nx, ny, nz);
            msg.send('moveTo', nx, ny, nz);
        };

        $scope.createInputMatrix = function(dx, dy, dz, roll, pitch, yaw) {
            msg.send('createInputMatrix', parseCoord(dx) || 0, parseCoord(dy) || 0, parseCoord(dz) || 0, parseCoord(roll) || 0, parseCoord(pitch) || 0, parseCoord(yaw) || 0);
        };
        
        
        mrl.subscribe($scope.service.name, 'publishJointPositions');
        mrl.subscribe($scope.service.name, 'publishJointAngles');
        mrl.subscribe($scope.service.name, 'publishJointAngle');
        mrl.subscribe($scope.service.name, 'publishWorldPosition');
        mrl.subscribe($scope.service.name, 'publishTracking');
        
        msg.subscribe('publishWorldPosition');
        msg.subscribe('publishIkGoal');
        msg.subscribe('publishJointPositions');
        msg.subscribe('publishJointAngles');
        msg.subscribe('moveTo');
        msg.subscribe('computePositionFromServos');
        msg.subscribe('calibrateFromSimulator');
        msg.subscribe('verifyAgainstSimulator');
        msg.subscribe(this);
    }]);
