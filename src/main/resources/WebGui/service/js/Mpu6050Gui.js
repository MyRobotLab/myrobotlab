angular.module('mrlapp.service.Mpu6050Gui', [])
.controller('Mpu6050GuiCtrl', ['$log', '$scope', 'mrl', function($log, $scope, mrl) {
    $log.info('Mpu6050GuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    // Init
    // Don't think init is necessary for data that is bound
    $scope.controllerName = '';
    $scope.controllers = [];  
    $scope.controllerLabel = 'Controller :';
    $scope.deviceBusLabel = 'Bus :';
    $scope.deviceAddressLabel = 'Address :';
    
    // Start of three.js scene and object creation
    // Create the scene
	$log.info("Creating scene..");
    var scene = new THREE.Scene();
    var camera = new THREE.PerspectiveCamera( 75, 1, 0.1, 1000 );
	var renderer = new THREE.WebGLRenderer();
	renderer.setSize(400,400);
	// Lightning
	var ambientLight, light;
	ambientLight = new THREE.AmbientLight( 0x333333 );	// 0.2
	light = new THREE.DirectionalLight( 0xFFFFFF, 1.0 );
	// Materials
	var teapotColor = new THREE.Color(0xdd00dd);
	phongMaterial = new THREE.MeshPhongMaterial( { color: teapotColor, shading: THREE.SmoothShading, side: THREE.DoubleSide } );
    var gridHelper = new THREE.GridHelper( 50, 5 );
    //
    var color1 = new THREE.Color(0xdd00dd);
    var color2 = new THREE.Color(0x00ff00);
    gridHelper.setColors(color1,color2);
    
    var teapotSize = 10;
    var tess = 15;
    var bottom = true;
    var lid = true;
    var body = true;
    var fitLid = true;
    var blinn = true;
    var teapotGeometry = new THREE.TeapotBufferGeometry( teapotSize,
			tess,
			bottom,
		    lid,
			body,
			fitLid,
			blinn);
    
    var teapot = new THREE.Mesh(teapotGeometry, phongMaterial);   
	var quarternion = new THREE.Quaternion(); 
		
    scene.add(ambientLight);
	scene.add(light);
    scene.add(teapot);
    // scene.add(gridHelper);

    camera.position.x = 0;
    camera.position.y = 8;
    camera.position.z = 50;

    renderer.render( scene, camera );
    // End of three.js scene creation and object creation  
    
    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service;
        $scope.controllers = service.controllers;
        $scope.controllerName = service.controllerName;
        $scope.deviceBusList = service.deviceBusList;
        $scope.deviceBus = service.deviceBus;
        $scope.deviceAddressList = service.deviceAddressList;
        $scope.deviceAddress = service.deviceAddress;
        
        $scope.isAttached = service.isAttached;
        
        $scope.accelGX = service.accelGX;
        $scope.accelGY = service.accelGY;
        $scope.accelGZ = service.accelGZ;
        
        $scope.temperatureC = service.temperatureC;

        $scope.gyroDegreeX = service.gyroDegreeX;
        $scope.gyroDegreeY = service.gyroDegreeY;
        $scope.gyroDegreeZ = service.gyroDegreeZ;

        $scope.filtered_x_angle = service.filtered_x_angle;
        $scope.filtered_y_angle = service.filtered_y_angle;
        $scope.filtered_z_angle = service.filtered_z_angle;
    }
    ;
    
    _self.updateState($scope.service);
    
    this.onMsg = function(inMsg) {
        var data = inMsg.data[0];
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data);
            $scope.$apply();
            var container = document.getElementById( 'canvas' );
    		// $log.info("CANVAS CONTAINER: " + container);
    		if (container.hasChildNodes()) {
    			container.removeChild( container.childNodes[0]);
    			container.appendChild( renderer.domElement );	
    		} else {
    			container.appendChild( renderer.domElement );
    		}
    		// Rotate the teapot based on the filtered x, y, z values
        	teapot.rotation.x = $scope.filtered_x_angle;
        	teapot.rotation.y = $scope.filtered_y_angle;
        	teapot.rotation.z = $scope.filtered_z_angle;
        	
            renderer.render( scene, camera );
            break;
        default:
            $log.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method);
            break;
        }
        ;
    
    }
    ;
    
    $scope.setControllerName = function(name) {
        $scope.controllerName = name;
    }
    
    $scope.setDeviceBus = function(bus) {
        $scope.deviceBus = bus;
    }
    
    $scope.setDeviceAddress = function(address) {
        $scope.deviceAddress = address;
    }
    
    // regrettably the onMethodMap dynamic
    // generation of methods failed on this overloaded
    // sweep method - there are several overloads in the
    // Java service - although msg.sweep() was tried for ng-click
    // for some reason Js resolved msg.sweep(null, null, null, null) :P

    msg.subscribe(this);
}
]);

