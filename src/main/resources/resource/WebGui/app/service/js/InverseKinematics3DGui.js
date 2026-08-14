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
        $scope.x = null;
        $scope.y = null;
        $scope.z = null;
        seedMoveTo($scope.worldPosition);
        // GOOD TEMPLATE TO FOLLOW
        this.updateState = function (service) {
            $scope.service = service;
            if (service && service.worldPosition) {
                $scope.worldPosition = service.worldPosition;
                seedMoveTo(service.worldPosition);
            }
        };

        function seedMoveTo(p) {
            if (!p) {
                return;
            }
            if ($scope.x === null || $scope.x === undefined || $scope.x === '') {
                $scope.x = roundCoord(p.x);
                $scope.y = roundCoord(p.y);
                $scope.z = roundCoord(p.z);
            }
        }

        function roundCoord(v) {
            if (v === null || v === undefined) {
                return v;
            }
            return Math.round(v * 10000) / 10000;
        }

        function toNumber(v) {
            var n = parseFloat(v);
            return isNaN(n) ? 0 : n;
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
		var axis = buildAxes( 500 );
		//var camera = new THREE.PerspectiveCamera( 75, window.innerWidth/window.innerHeight, 0.1, 1000 );
		var camera = new THREE.PerspectiveCamera( 75, 1, 0.1, 1000 );
		camera.position.x = 50;
		camera.position.y = -200;
		camera.position.z = 500;
		camera.lookAt(new THREE.Vector3(0, 0, 0));
		bindCanvasOrbit(renderer, camera);

		function bindCanvasOrbit(webglRenderer, cam) {
			var el = webglRenderer.domElement;
			if (el._ikOrbitBound) {
				return;
			}
			el._ikOrbitBound = true;
			el.style.cursor = 'grab';
			el.title = 'Left-drag to orbit, wheel to zoom';

			var lookAt = new THREE.Vector3(0, 0, 0);
			var dragging = false;
			var lastX = 0;
			var lastY = 0;
			var radius = cam.position.length();
			var theta = Math.atan2(cam.position.x, cam.position.z);
			var phi = Math.acos(Math.max(-1, Math.min(1, cam.position.y / Math.max(radius, 1e-6))));

			function applyOrbit() {
				phi = Math.max(0.05, Math.min(Math.PI - 0.05, phi));
				radius = Math.max(40, Math.min(2500, radius));
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
                    $scope.worldPosition = msg.data[0];
                    seedMoveTo($scope.worldPosition);
                    $scope.$apply();
                    break;
                case 'onComputePositionFromServos':
                    if (msg.data && msg.data[0]) {
                        $scope.worldPosition = msg.data[0];
                        $scope.x = roundCoord(msg.data[0].x);
                        $scope.y = roundCoord(msg.data[0].y);
                        $scope.z = roundCoord(msg.data[0].z);
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

        $scope.moveTo = function(x, y, z) {
            var nx = roundCoord(toNumber(x));
            var ny = roundCoord(toNumber(y));
            var nz = roundCoord(toNumber(z));
            $log.info('moveTo', nx, ny, nz);
            msg.send('moveTo', nx, ny, nz);
        };

        $scope.createInputMatrix = function(dx, dy, dz, roll, pitch, yaw) {
            msg.send('createInputMatrix', toNumber(dx), toNumber(dy), toNumber(dz), toNumber(roll), toNumber(pitch), toNumber(yaw));
        };
        
        
        mrl.subscribe($scope.service.name, 'publishJointPositions');
        mrl.subscribe($scope.service.name, 'publishJointAngles');
        mrl.subscribe($scope.service.name, 'publishJointAngle');
        mrl.subscribe($scope.service.name, 'publishWorldPosition');
        mrl.subscribe($scope.service.name, 'publishTracking');
//        $scope.panel.initDone();
        
        msg.subscribe('publishWorldPosition');
        msg.subscribe('publishJointPositions');
        msg.subscribe('publishJointAngles');
        msg.subscribe('computePositionFromServos');
        msg.subscribe(this);
    }]);
