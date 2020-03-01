angular.module('mrlapp.service.InMoov2Gui', []).controller('InMoov2GuiCtrl', ['$scope', 'mrl', function($scope, mrl) {
    console.info('InMoov2GuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.servos = []
    $scope.sliders = []

    // text published from InMoov2 service
    $scope.onText = null
    $scope.languageSelected = null
    $scope.speakText = null
    $scope.toggleValue = true

    $scope.activePanel = 'settings'

    $scope.speechTypes = null
    $scope.mouth = null

    $scope.selectedButton = {}

    $scope.selectedGesture = null

    // inmoov menu buttons
    $scope.buttons = []

    let addButton = function(name) {
        let button = {
            name: name,
            translate: "0px,0px",
            img: "../InMoov2/img/" + name + "_off.png",
            hover: "../InMoov2/img/" + name + "_hover.png"
        }
        $scope.buttons.push(button)
    }

    let calculatButtonPos = function() {
        let angle = 234.5 * (Math.PI / 180)// (360 - 90) * (Math.PI / 180)
        let dangle = (360 / $scope.buttons.length) * (Math.PI / 180)
        let centerX = 238
        let centerY = 226
        let radius = 230
        for (i = 0; i < $scope.buttons.length; i++) {
            angle += dangle
            // $scope.buttons[i].rotate = angle + "deg"
            var x = Math.round(centerX + radius * Math.cos(angle));
            var y = Math.round(centerY + radius * Math.sin(angle));
            $scope.buttons[i].translate = x +"px,"+ y + "px"
        }
    }

    let highlightButton = function(name){

        if (name == 'InMoov'){
            
        }

        // FIXME - won't work - need to have a selected button that overlays !!!
        for (i = 0; i < $scope.buttons.length; i++) {
           if ($scope.buttons[i].name == name){
               // $scope.buttons[i].img = "../InMoov2/img/" + name + "_on.png"
               $scope.selectedButton.name = name
               $scope.selectedButton.translate = $scope.buttons[i].translate
               $scope.selectedButton.img =  "../InMoov2/img/" + name + "_on.png"
               break;
               
           } 
        }
        // $scope.$apply()
        console.info('here')
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.languageSelected = service.locale.tag

        $scope.mouth = mrl.getService(service.name + '.mouth')
        $scope.$apply()
    }

    $scope.getShortName = function(longName) {
        return longName.substring(longName.lastIndexOf(".") + 1)
    }

    $scope.toggle = function(servo) {
        $scope.sliders[servo].tracking = !$scope.sliders[servo].tracking
    }

    _self.onSliderChange = function(servo) {
        if (!$scope.sliders[servo].tracking) {
            msg.sendTo(servo, 'moveTo', $scope.sliders[servo].value)
        }
    }

    $scope.active = ["btn", "btn-default", "active"]

    $scope.executeGesture = function(gesture) {
        msg.send('execGesture', gesture);
    }

    $scope.setActive = function(val) {
        var index = array.indexOf(5);
        if (index > -1) {
            array.splice(index, 1);
        }
    }

    $scope.getStyle = function(bool) {
        // return ['btn', 'btn-default', 'active']
        return 'active';
        // return mrl.getStyle(bool)
    }

    $scope.getPeer = function(peerName) {
        let s = mrl.getService($scope.service.name + '.' + peerName + '@' + this.service.id)
        return s
    }

    $scope.startMouth = function() {
        msg.send('setSpeechType', $scope.speechTypeSelected)
        msg.send('startMouth')
    }

    $scope.speak = function() {
        if ($scope.mouth == null) {
            $scope.startMouth()
        }
        msg.send('speakBlocking', $scope.speakText)
    }

    $scope.setPanel = function(panelName) {
        $scope.activePanel = panelName
        highlightButton(panelName)
    }

    $scope.showPanel = function(panelName) {
        return $scope.activePanel == panelName
    }

    this.onMsg = function(inMsg) {
        let data = inMsg.data[0];

        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
        case 'onServiceTypeNamesFromInterface':
            $scope.speechTypes = data.serviceTypes;
            $scope.$apply()
            break
        case 'onText':
            $scope.onText = data;
            $scope.$apply()
            break
        case 'onServoData':

            $scope.sliders[data.name].value = data.pos;
            $scope.$apply()
            break
        case 'onServoNames':
            // servos sliders are either in "tracking" or "control" state
            // "tracking" they are moving from callback position info published by servos
            // "control" they are sending control messages to the servos
            $scope.servos = inMsg.data[0]
            for (var servo of $scope.servos) {
                // dynamically build sliders
                $scope.sliders[servo] = {
                    value: 0,
                    tracking: true,
                    options: {
                        id: servo,
                        floor: 0,
                        ceil: 180,
                        onStart: function(id) {},
                        onChange: function(id) {
                            _self.onSliderChange(id)
                        },
                        /*
                        onChange: function() {
                            if (!this.tracking) {
                                // if not tracking then control
                                msg.sendTo(servo, 'moveToX', sliders[servo].value)
                            }
                        },*/
                        onEnd: function(id) {}
                    }
                }
                // dynamically add callback subscriptions
                // these are "intermediate" subscriptions in that they
                // don't send a subscribe down to service .. yet 
                // that must already be in place (and is in the case of Servo.publishServoData)
                msg.subscribeTo(_self, servo, 'publishServoData')

            }
            $scope.$apply()
            break
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    // msg.subscribe('getServoNames')
    // msg.send('getServoNames')
    // mrl.subscribeToServiceMethod(_self.onMsg, mrl.getRuntime().name, 'getServiceTypeNamesFromInterface');

    addButton('brain')
    addButton('mouth')
    addButton('head')
    addButton('torso')
    addButton('extra')
    addButton('leg')
//    addButton('InMoov')
    addButton('sensor')
    addButton('arm')
    addButton('hand')
    addButton('ear')
    
    calculatButtonPos()

    $scope.setPanel('InMoov')

    /*

    msg.subscribeTo(_self, mrl.getRuntime().name, 'getServiceTypeNamesFromInterface')
    msg.subscribe('getServiceTypeNamesFromInterface')
    */

    // FIXME FIXME FIXME - single simple subscribeTo(name, method) !!!
    mrl.subscribe(mrl.getRuntime().name, 'getServiceTypeNamesFromInterface');
    mrl.subscribeToServiceMethod(_self.onMsg, mrl.getRuntime().name, 'getServiceTypeNamesFromInterface');

    msg.subscribe('publishText')
    msg.sendTo(mrl.getRuntime().name, 'getServiceTypeNamesFromInterface', 'SpeechSynthesis')
    msg.subscribe(this)
}
])
