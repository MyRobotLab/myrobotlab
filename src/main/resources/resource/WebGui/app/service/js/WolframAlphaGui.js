angular.module('mrlapp.service.WolframAlphaGui', []).controller('WolframAlphaGuiCtrl', ['$sce','$scope', 'mrl', '$uibModal', '$sce', '$compile', '$uibModal', function($sce, $scope, mrl, $uibModal, $sce, $compile, $uibModal) {
    // $modal ????
    console.info('WolframAlphaGuiCtrl')
    // grab the self and message
    let _self = this
    let msg = this.msg

    $scope.rows = []
    $scope.searchText = null
    $scope.trustAsHtml = $sce.trustAsHtml;

    $scope.test = '<html><body><br><b>Input interpretation</b><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11091h54b2aei382g0180000234fcig2168bge5b?MSPStoreType=image/gif&s=9"><br><br><b>Scientific name</b><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11101h54b2aei382g018000014c4d08966830667?MSPStoreType=image/gif&s=9"><br><br><b>Alternate common name</b><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11111h54b2aei382g018000034i6277358748c30?MSPStoreType=image/gif&s=9"><br><br><b>Taxonomy</b><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11121h54b2aei382g018000055ig9i6e70h5hb68?MSPStoreType=image/gif&s=9"><br><br><b>Biological properties</b><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11131h54b2aei382g018000023e406hh782f0i89?MSPStoreType=image/gif&s=9"><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11141h54b2aei382g018000035b28hab584g92bi?MSPStoreType=image/gif&s=9"><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11151h54b2aei382g018000054cdb5d9adhd297i?MSPStoreType=image/gif&s=9"><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11171h54b2aei382g01800005314ia754dde67f8?MSPStoreType=image/gif&s=9"><br><br><b>Image</b><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11181h54b2aei382g01800005dd0ff93c9675gbf?MSPStoreType=image/gif&s=9"><br><br><b>Other members of phylum Chordata</b><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11191h54b2aei382g01800004g5ci93130g36e5h?MSPStoreType=image/gif&s=9"><br><br><b>Members of class Aves</b><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11201h54b2aei382g01800002aif4f6fi10hec1e?MSPStoreType=image/gif&s=9"><br><br><b>Taxonomic network</b><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11211h54b2aei382g0180000658bg42i2dgeh61f?MSPStoreType=image/gif&s=9"><br><br><b>Wikipedia summary</b><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11221h54b2aei382g018000050bh7g79141f2e7g?MSPStoreType=image/gif&s=9"><br><br><b>Wikipedia page hits history</b><br><img src="https://www6b3.wolframalpha.com/Calculate/MSP/MSP11231h54b2aei382g01800000gh5ied841259f0h?MSPStoreType=image/gif&s=9"><br></body><html>'

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
        console.info("WolframAlphaGui.onMsg(" + inMsg.method + ')')
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
