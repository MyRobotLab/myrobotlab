angular.module('mrlapp.service')
.directive(
"tjsModelViewer", 
[function() {
    return {
        restrict: "E",
        scope: {
            assimpUrl: "=assimpUrl"
        },
        link: function(scope, elem, attr) {
            var camera;
            var scene;
            var renderer;
            var previous;
            
            // init scene
            init();
            
            // Load jeep model using the AssimpJSONLoader
            //var loader1 = new THREE.AssimpJSONLoader();
            var loader2 = new new THREE.JSONLoader();
            
            /*

					loader.load('./model.js', function (geometry, materials) {
                    var skinnedMesh = new THREE.SkinnedMesh(geometry, new THREE.MeshFaceMaterial(materials));
                    skinnedMesh.position.y = 50;
                    skinnedMesh.scale.set(15, 15, 15);
                    scene.add(skinnedMesh);

                    animate(skinnedMesh);
                   });

            */
            
            scope.$watch("assimpUrl", function(newValue, oldValue) {
                if (newValue != oldValue)
                    loadModel(newValue);
            });
            
            function loadModel(modelUrl) {
                
                loader2.load(modelUrl, function (geometry, materials) {
                    var skinnedMesh = new THREE.SkinnedMesh(geometry, new THREE.MeshFaceMaterial(materials));
                    skinnedMesh.position.y = 50;
                    skinnedMesh.scale.set(15, 15, 15);
                    scene.add(skinnedMesh);

                    //animate(skinnedMesh);
                   });
/*
                loader1.load(modelUrl, function(assimpjson) {
                    assimpjson.scale.x = assimpjson.scale.y = assimpjson.scale.z = 0.2;
                    assimpjson.updateMatrix();
                    if (previous)
                        scene.remove(previous);
                    scene.add(assimpjson);
                    
                    previous = assimpjson;
                });
                */
            }
            
            loadModel(scope.assimpUrl);
            animate();
            
            function init() {
                camera = new THREE.PerspectiveCamera(50,window.innerWidth / window.innerHeight,1,2000);
                camera.position.set(2, 4, 5);
                scene = new THREE.Scene();
                scene.fog = new THREE.FogExp2(0x000000,0.035);
                // Lights
                scene.add(new THREE.AmbientLight(0xcccccc));
                var directionalLight = new THREE.DirectionalLight(/*Math.random() * 0xffffff*/0xeeeeee);
                directionalLight.position.x = Math.random() - 0.5;
                directionalLight.position.y = Math.random() - 0.5;
                directionalLight.position.z = Math.random() - 0.5;
                directionalLight.position.normalize();
                scene.add(directionalLight);
                
                var scalar = 4;
                
                // Renderer
                renderer = new THREE.WebGLRenderer();
                renderer.setSize(window.innerWidth / scalar, window.innerHeight / scalar);
                // FIXME - some attribute of the serviceGui
                //renderer.setSize(600, 337); 
                elem[0].appendChild(renderer.domElement);
                
                // Events
                window.addEventListener('resize', onWindowResize, false);
            }
            
            //
            function onWindowResize(event) {
                var scalar = 4;
                renderer.setSize(window.innerWidth / scalar, window.innerHeight / scalar);
                camera.aspect = window.innerWidth / window.innerHeight;
                camera.updateProjectionMatrix();
            }
            
            //
            var t = 0;
            
            function animate() {
                requestAnimationFrame(animate);
                render();
            }
            
            //
            function render() {
                var timer = Date.now() * 0.0005;
                camera.position.x = Math.cos(timer) * 10;
                camera.position.y = 4;
                camera.position.z = Math.sin(timer) * 10;
                camera.lookAt(scene.position);
                renderer.render(scene, camera);
            }
        }
    }
}
]);
