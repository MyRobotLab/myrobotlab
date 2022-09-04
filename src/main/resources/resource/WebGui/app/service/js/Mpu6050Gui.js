angular.module('mrlapp.service.Mpu6050Gui', []).controller('Mpu6050GuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('Mpu6050GuiCtrl')
    var _self = this
    var msg = this.msg

    $scope.controllers = []

    // published Mpu6050 data
    $scope.data = null
    $scope.orientation = {
        "pitch": 0,
        "roll": 0,
        "yaw": 0
    }

    $scope.sampleRates = []

    for (let i = 1; i < 20; ++i) {
        $scope.sampleRates.push(i)
    }

    // Start of three.js scene and object creation
    // Create the scene
    console.info("Creating scene..")
    var scene = new THREE.Scene()
    var camera = new THREE.PerspectiveCamera(25,1,0.5,1000)
    var renderer = new THREE.WebGLRenderer()
    renderer.setSize(400, 400)
    // Lightning
    var ambientLight, light
    ambientLight = new THREE.AmbientLight(0x333333)
    // 0.2
    light = new THREE.DirectionalLight(0xFFFFFF,1.0)
    // Materials
    var teapotColor = new THREE.Color(0xdd00dd)
    phongMaterial = new THREE.MeshPhongMaterial({
        color: teapotColor,
        shading: THREE.SmoothShading,
        side: THREE.DoubleSide
    })
    var gridHelper = new THREE.GridHelper(50,5)
    //
    var color1 = new THREE.Color(0xdd00dd)
    var color2 = new THREE.Color(0x00ff00)
    gridHelper.setColors(color1, color2)

    var teapotSize = 10
    var tess = 15
    var bottom = true
    var lid = true
    var body = true
    var fitLid = true
    var blinn = true

    const geometry = new THREE.BoxGeometry(10,10,10)
    // const material = new THREE.MeshBasicMaterial( {color: 0x00ff00} )
    const material = new THREE.MeshBasicMaterial({
        color: 0xffffff,
        vertexColors: true
    })
    const teapot = new THREE.Mesh(geometry,material)

    for (var i = 0; i < geometry.faces.length; i++) {
        geometry.faces[i].color.setHex(Math.random() * 0xffffff)
    }

    var quarternion = new THREE.Quaternion()

    scene.add(ambientLight)
    scene.add(light)
    scene.add(teapot)
    // scene.add(gridHelper)

    camera.position.x = 0
    camera.position.y = 0
    camera.position.z = 40

    renderer.render(scene, camera)
    // End of three.js scene creation and object creation  

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            var container = document.getElementById('canvas')
            // console.info("CANVAS CONTAINER: " + container)
            if (container.hasChildNodes()) {
                container.removeChild(container.childNodes[0])
                container.appendChild(renderer.domElement)
            } else {
                container.appendChild(renderer.domElement)
            }
            // Rotate the teapot based on the filtered x, y, z values
            teapot.rotation.x = $scope.filtered_x_angle
            teapot.rotation.y = $scope.filtered_y_angle
            teapot.rotation.z = $scope.filtered_z_angle

            renderer.render(scene, camera)
            break
        case 'onMpu6050Data':
            var container = document.getElementById('canvas')
            // console.info("CANVAS CONTAINER: " + container)
            if (container.hasChildNodes()) {
                container.removeChild(container.childNodes[0])
                container.appendChild(renderer.domElement)
            } else {
                container.appendChild(renderer.domElement)
            }

            // Rotate the teapot based on the filtered x, y, z values
            teapot.rotation.x = data.orientation.pitch
            teapot.rotation.y = data.orientation.roll
            teapot.rotation.z = data.orientation.yaw

            $scope.data = data

            $scope.$apply()
            renderer.render(scene, camera)
            break
        case 'onStatus':
        default:
            console.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }

    }

    msg.subscribe('publishMpu6050Data')
    // msg.subscribe('publishOrientation')
    msg.subscribe(this)
}
])
