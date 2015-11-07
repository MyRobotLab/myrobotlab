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
        
        
        // GOOD TEMPLATE TO FOLLOW
        this.updateState = function (service) {
            $scope.service = service;
        };

        _self.updateState($scope.service);

        
        this.onMsg = function(msg) {
            $log.info("On Message IK3D!");
            $log.info(msg);
            switch (msg.method) {
                case 'onJointPositions':
                	$log.info("On Joint Positions..");
                    $scope.positions = msg.data[0];
                    $scope.$apply();
                    
            		// Our Javascript will go here.
            		// ref: http://soledadpenades.com/articles/three-js-tutorials/drawing-the-coordinate-axes/
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

            		// Initialize the scene.
            		var scene = new THREE.Scene();

            		var container = document.getElementById( 'canvas' );
            		// document.body.appendChild( container );

            		//  TODO: how do we tell it to use the container?
            		var renderer = new THREE.WebGLRenderer();
            		renderer.setSize( 400, 400 );
            		//container.appendChild( renderer.domElement );

            		// var renderer = new THREE.WebGLRenderer();


            		//renderer.setSize( window.innerWidth, window.innerHeight );
            		// renderer.setSize( 400,400);
            		if (container.hasChildNodes()) {
            			container.removeChild( container.childNodes[0]);
            			container.appendChild( renderer.domElement );	
            		} else {
            			container.appendChild( renderer.domElement );
            		}
            		
            		// startpoint/stoppoint for each link as array  [ [ x1,y1,z1 ] , [ x2,y2,z2 ] ] stop x,y,z  points.
            		// This is the start/stop positions of all the joints
            		
            		 console.log($scope.positions);
            		 var linkPoints = [];
            		 for (i = 0 ; i < $scope.positions.length-1; i++) {
            			 console.log("Push a point!");
            			 console.log(i);
            			 console.log($scope.positions.length);
            			 console.log("Push a point 2!");
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
            			
            			var startPoint = linkPoints[i][0];
            			var stopPoint = linkPoints[i][1];
            			console.log("Here!" + startPoint + " TO " + stopPoint); 
            			
            			var color = colorHex[i%numColors];
            			var start = new THREE.Vector3( startPoint[0], startPoint[1], startPoint[2] );
            			var stop = new THREE.Vector3( stopPoint[0], stopPoint[1], stopPoint[2] );
            			var lineGeom = new THREE.Geometry();
            		    var lineMat = new THREE.LineBasicMaterial({ linewidth: 3, color: color });
            		    lineGeom.vertices.push( start );
            		    lineGeom.vertices.push( stop );
            		    // lineGeom.computeLineDistances(); // This one is SUPER important, otherwise dashed lines will appear as simple plain lines
            		    var segment = new THREE.Line( lineGeom, lineMat, THREE.LinePieces );
            			scene.add( segment.clone() );
            		}; 

            		// add the x,y,z axis lines to the scene.
            		var axis = buildAxes( 500 );
            		scene.add( axis );
            		// Set up the camera.

            		//var camera = new THREE.PerspectiveCamera( 75, window.innerWidth/window.innerHeight, 0.1, 1000 );
            		var camera = new THREE.PerspectiveCamera( 75, 1, 0.1, 1000 );
            		camera.position.x = 50;
            		camera.position.y = 50;
            		camera.position.z = 500;
            		var render = function () {
            			requestAnimationFrame( render );
            			//cube.rotation.x += 0.1;
            			// cube.rotation.y += 0.1;
            			//cube.position.x = 5;
            			renderer.render(scene, camera);
            		};
            		render();
            				
                    
                    break;
                case 'onJointAngles':
                	$log.info("On Joint Angles..");
                	$scope.angles = msg.data[0];
                	$scope.$apply();
                	break;
                default:
                    $log.error("ERROR - unhandled method " + $scope.name + " " + msg.method);
                    break;
            };
        };
        
        $scope.moveTo = function(x,y,z) {
    	  // Invoke the moveTo..
          $log.info("MOVETO! IK3D!");
          mrl.sendTo($scope.service.name, "moveTo", x, y, z);
          
        };
        mrl.subscribe($scope.service.name, 'publishJointPositions');
        mrl.subscribe($scope.service.name, 'publishJointAngles');
//        $scope.panel.initDone();
        
        msg.subscribe(this);
    }]);
