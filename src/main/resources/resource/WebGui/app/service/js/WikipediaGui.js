angular.module('mrlapp.service.WikipediaGui', []).controller('WikipediaGuiCtrl', ['$scope', 'mrl', '$uibModal', '$sce', '$compile', '$uibModal', function($scope, mrl, $uibModal, $sce, $compile, $uibModal) {
    // $modal ????
    console.info('WikipediaGuiCtrl')
    // grab the self and message
    let _self = this
    let msg = this.msg

    $scope.rows = []
    $scope.searchText = null

    // following the template.
    this.updateState = function(service) {
        // use another scope var to transfer/merge selection
        // from user - service.currentSession is always read-only
        // all service data should never be written to, only read from

        $scope.service = service
    }

    $scope.search = function() {

        // $scope.rows.unshift($scope.searchText)
        msg.send('search', $scope.searchText)
        $scope.searchText = ''
    }

    this.onMsg = function(inMsg) {
        console.info("WikipediaGui.onMsg(" + inMsg.method + ')')
        let data = inMsg.data[0]

        switch (inMsg.method) {

        case 'onState':
            _self.updateState(data)
            $scope.$apply()
            break

        case 'onText':
            $scope.rows.unshift(data)
            $scope.$apply()
            break

        case 'onImage':
            $scope.rows.unshift(data)
            $scope.$apply()
            break

        case 'onResults':
            $scope.rows.unshift(data)
            $scope.$apply()
            break

        default:
            console.error("ERROR - unhandled method " + $scope.name + " " + inMsg.method)
            break
        }

    }

    $scope.showAdvanced = function(ev) {
        var modalInstance = $uibModal.open({
                         template: '<img src="https://static01.nyt.com/images/2020/02/13/world/13uk-plane/13uk-plane-articleLarge.jpg" width="100%" />',
                    //  templateUrl: 'view/sample.html',
                     // controller: 'testController',// a controller for modal instance
                     // controllerUrl: 'controller/test-controller', // can specify controller url path
                     controllerAs: 'ctrl', //  controller as syntax
                     windowClass: 'clsPopup', //  can specify the CSS class
                     keyboard: false, // ESC key close enable/disable
                     resolve: {
                         actualData: function () {
                             return self.sampleData
                         }
                     } // data passed to the controller
                 }).result.then(function (data) {
                     //do logic
                 }, function () {
                     // action on popup dismissal.
                 })
    }
    

    // subscribe to the response from programab.

    // msg.subscribe('publishText')
    // msg.subscribe('publishImages')
    msg.subscribe('publishResults')
    msg.subscribe(this)
}
])
