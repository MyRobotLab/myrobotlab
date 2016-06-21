angular.module('mrlapp.service.Mpu6050Gui', [])
.controller('Mpu6050GuiCtrl', ['$log', '$scope', 'mrl', function($log, $scope, mrl) {
    $log.info('Mpu6050GuiCtrl');
    var _self = this;
    var msg = this.msg;
    
    // Init
    // Don't think init is necessary for data that is bound
    $scope.controllerName = '';
    $scope.controllers = [];  
    // Start of three.js scene and object creation
    // Create the scene
	$log.info("Creating scene..");
    var scene = new THREE.Scene();
    var camera = new THREE.PerspectiveCamera( 75, 1, 0.1, 1000 );
	var renderer = new THREE.WebGLRenderer();
	renderer.setSize(400,400);
    // TODO: Bind with html in the right place, not the body
    // document.body.appendChild( renderer.domElement );
    // var container = document.getElementById("canvas");
    /*
    if (container.hasChildNodes()) {
		container.removeChild( container.childNodes[0]);
		container.appendChild( renderer.domElement );	
	} else {
		container.appendChild( renderer.domElement );
	}
    // Create the objects in the scene
    */
    var geometry = new THREE.BoxGeometry(10, 0.5, 0.5);
    var material = new THREE.MeshBasicMaterial({color: 0xff0000});
    var cube = new THREE.Mesh(geometry,material);
    
    var geometry2 = new THREE.BoxGeometry(0.5, 0.5, 10);
    var material2 = new THREE.MeshBasicMaterial({color: 0x00ff00});
    var cube2 = new THREE.Mesh(geometry2,material2);
    
    var gridHelper = new THREE.GridHelper( 50, 5 );
    var gray  = new THREE.Color(0xdd00dd);
    var black = new THREE.Color(0x00ff00);
    gridHelper.setColors(gray,black);
    
    scene.add(cube);
    scene.add(cube2);
    scene.add(gridHelper);

    camera.position.x = 0;
    camera.position.y = 3;
    camera.position.z = 10;
    /*
    function animate() {
        requestAnimationFrame( animate );
        controls.update();
        renderer.render( scene, camera );
    	}
    animate();
    */
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
        	cube.rotation.x = $scope.gyroDegreeX / (2 * Math.PI);
        	cube.rotation.y = $scope.gyroDegreeY / (2 * Math.PI);
        	cube.rotation.z = $scope.gyroDegreeZ / (2 * Math.PI);

        	cube2.rotation.x = $scope.gyroDegreeX / (2 * Math.PI);
        	cube2.rotation.y = $scope.gyroDegreeY / (2 * Math.PI);
        	cube2.rotation.z = $scope.gyroDegreeZ / (2 * Math.PI);
        	
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
