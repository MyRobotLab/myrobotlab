angular.module('mrlapp.service.IntroGui', []).controller('IntroGuiCtrl', ['$scope', 'mrl', '$timeout', function($scope, mrl, $timeout) {
    console.info('IntroGuiCtrl')
    var _self = this
    var msg = this.msg
    $scope.mrl = mrl
    // $scope.panel = mrl.getPanel('runtime')
    // although this should be a set or dictionary - angular does not handle key changes
    // correctly on modifications - instead the only handle array[] changes :(
    $scope.panels = []
    $scope.props = {}

    $scope.subPanels = {}

    // possible panels of interest
    let panelNames = new Set()
    panelNames.add('arduino')
    panelNames.add('servo01')
    panelNames.add('servo02')
    panelNames.add('python')
    panelNames.add('security')
    panelNames.add('runtime')

    // GOOD pattern to follow - updateState called form onMsg
    this.updateState = function(service) {
        $scope.service = service
        $scope.props = service.props
        console.info($scope.props.servo01IsActive)
    }

    this.onMsg = function(inMsg) {
        var data = inMsg.data[0]
        switch (inMsg.method) {
        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break
            // servo event in the past 
            // meant feedback from MRLComm.c
            // but perhaps its come to mean
            // feedback from the service.moveTo
        case 'onStatus':
            console.log(data)
            $scope.$apply()
            break

        default:
            console.info("ERROR - unhandled method " + $scope.name + " Method " + inMsg.method)
            break
        }
    }

    $scope.setPanel = function(panelName) {
        $scope.activePanel = panelName
    }

    $scope.showPanel = function(panelName) {
        return $scope.activePanel == panelName
    }

    $scope.setPanel('extension')

    $scope.get = function(key) {
        let ret = $scope.service.props[key]
        return ret
    }

    $scope.start = function(name, type) {
        msg.sendTo('runtime', 'start', name, type)
    }


    // this method initializes subPanels when a new service becomes available
    this.onRegistered = function(panel) {
        if (panelNames.has(panel.displayName)) {
            $scope.subPanels[panel.displayName] = panel
        }
    }

    // this method removes subPanels references from released service
    this.onReleased = function(panelName) {
        if (panelNames.has(panelName)) {
            $scope.subPanels[panelName]           
        }
    }

    // initialize all services which have panel references in Intro
    let servicePanelList = mrl.getPanelList()
    for (let index = 0; index < servicePanelList.length; ++index){
        this.onRegistered(servicePanelList[index])
    }

    msg.subscribe(this)
    // runtime registration of new services
    mrl.subscribeToRegistered(this.onRegistered)
    mrl.subscribeToReleased(this.onReleased)
}
])
