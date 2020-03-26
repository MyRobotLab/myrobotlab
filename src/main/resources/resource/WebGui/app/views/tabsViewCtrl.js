angular.module('mrlapp.mrl').controller('tabsViewCtrl', ['$scope', '$log', '$filter', '$timeout', 'mrl', '$state', '$stateParams', function($scope, $log, $filter, $timeout, mrl, $state, $stateParams) {
    $log.info('tabsViewCtrl $scope.$id - ' + $scope.$id)

    $scope.view_tab = 'default'
    $scope.viewType = mrl.getViewType()
    $scope.servicename = $stateParams.servicename
    $scope.servicename = 'runtime@inmoov'

    // setting callback method in service so other controllers
    // can set searchText
    $scope.setSearchText = function(text) {
        $scope.searchText.displayName = text
    }

    $scope.noworky = function() {
        noWorkySvc.openNoWorkyModal($scope.panel.name)
    }

    $scope.updateServiceData = function() {
        //get an updated / fresh servicedata & convert it to json
        var servicedata = mrl.getService($scope.view_tab)
        $scope.servicedatajson = JSON.stringify(servicedata, null, 2)
    }

    /*
    $scope.wildCardOn = true

    $scope.checkForWildCard = function(value){
        console.info(value)
        if (value.displayName && value.displayName.endsWith('.')){
            $scope.wildCardOn = false
        } else {
            $scope.wildCardOn = false
        }
    }*/

    $scope.getName = function(panel) {
        return panel.name
    }

    //service-panels & update-routine
    var panelsUpdated = function(panels) {
        console.debug('tabsViewCtrl.panelsUpdated ' + panels.length)
        $scope.panels = panels

        if ($scope.view_tab == 'default' && $scope.panels && $scope.panels[0]) {
            $scope.view_tab = $scope.panels[0].name
        }

        if ($scope.servicename) {
            $scope.changeTab(mrl.getFullName($scope.servicename))
        }
    }

    $scope.changeTab = function(tab) {
        $scope.view_tab = tab
        $timeout(function() {
            // lame hack rzSlider recommened
            $scope.$broadcast('rzSliderForceRender')
        })
    }


    $scope.searchText = {
        // displayName: ""
    }

    panelsUpdated(mrl.getPanelList())
    mrl.setSearchFunction($scope.setSearchText)
    mrl.subscribeToUpdates(panelsUpdated)
}
])
