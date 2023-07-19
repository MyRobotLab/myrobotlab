angular.module('mrlapp.service.SpotMicroGui', []).controller('SpotMicroGuiCtrl', ['$scope', 'mrl', 'peer', function($scope, mrl, peer) {
    console.info('SpotMicroGuiCtrl')
    var _self = this
    var msg = this.msg
    
    $scope.peer = peer
    $scope.mrl = mrl
    $scope.panel = mrl.getPanel('runtime')
    $scope.onText = null
    $scope.activePanel = 'settings'

    $scope.isSpotMicro = function() {
        if ($scope.service){
            return $scope.service.name.includes("spotMicro")
        }
        return false
    }

    $scope.filterPeers = function(peerName) {
        if (peerName) {
            mrl.search($scope.service.name + '.' + peerName)
        } else {
            mrl.search("")
        }
    }

    // GOOD TEMPLATE TO FOLLOW
    this.updateState = function(service) {
        $scope.service = service
        $scope.$apply()
    }

    $scope.getShortName = function(longName) {
        return longName.substring(longName.lastIndexOf(".") + 1)
    }

    $scope.active = ["btn", "btn-default", "active"]


    $scope.setActive = function(val) {
        var index = array.indexOf(5);
        if (index > -1) {
            array.splice(index, 1);
        }
    }

    $scope.getPeer = function(peerName) {
        let s = mrl.getService($scope.service.name + '.' + peerName + '@' + this.service.id)
        return s
    }

    $scope.peerState = function(peerKey) {
        $scope.service.serviceType.peers[peerKey].state
    }

    $scope.startPeer = function(peerKey) {
        console.info(peerKey)
        console.info($scope.msg)
        msg.send('startPeer', peerKey)
    }

    $scope.releasePeer = function(peerKey) {
        console.info(peerKey)
        msg.send('releasePeer', peerKey)
    }


    $scope.setPanel = function(panelName) {
        $scope.activePanel = panelName

        // unselect active buttons by removing active class
        var container = document.querySelector("#spot-container");
        if (container!=null) {
            var matchesItems = container.querySelectorAll(".dotSpotActive");
            for (var i = 0; i < matchesItems.length; i++) { matchesItems[i].classList.remove('dotSpotActive'); }

            var matchesItems = container.querySelectorAll(".dotSpotButtonsActive");
            for (var i = 0; i < matchesItems.length; i++) { matchesItems[i].classList.remove('dotSpotButtonsActive'); matchesItems[i].classList.add('dotSpotButtons'); }
        }     

        // add activ class to dot ans button object
        if (document.querySelector("#"+panelName+"Dot")!=null) {
            document.querySelector("#"+panelName+"Dot").classList.add('dotSpotActive');
            document.querySelector("#"+panelName+"Button").classList.add('dotSpotButtonsActive');
        }   

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
            
        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }
    }

    $scope.execScript = function(resourceScript) {
        console.info('execScript')
        msg.send('execScript', resourceScript)
    } 


    $scope.setPanel('spotMicro')

    // FIXME FIXME FIXME - single simple subscribeTo(name, method) !!!
    mrl.subscribe(mrl.getRuntime().name, 'getServiceTypeNamesFromInterface');
    mrl.subscribeToServiceMethod(_self.onMsg, mrl.getRuntime().name, 'getServiceTypeNamesFromInterface');
    msg.subscribe('publishPeerStarted')
    msg.subscribe('publishText')
    msg.subscribe(this)
}
])
