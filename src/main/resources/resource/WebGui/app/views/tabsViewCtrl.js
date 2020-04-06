angular.module('mrlapp.mrl').controller('tabsViewCtrl', ['$scope', '$log', '$filter', '$timeout', 'mrl', '$state', '$stateParams', function($scope, $log, $filter, $timeout, mrl, $state, $stateParams) {
    $log.info('tabsViewCtrl $scope.$id - ' + $scope.$id)
    _self = this

    $scope.view_tab = null
    // $scope.viewType = mrl.getViewType()
    $scope.servicename = $stateParams.servicename

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

    $scope.getName = function(panel) {
        return panel.name
    }

    //service-panels & update-routine
    var panelsUpdated = function(panels) {
        console.debug('tabsViewCtrl.panelsUpdated ' + panels.length)
        $scope.panels = panels

        if (!$scope.view_tab && panels.length > 0 && $scope.panels[$scope.panels.length - 1].name.startsWith('intro')) {
            $scope.changeTab($scope.panels[0].name)
        }

        if ($scope.servicename) {
            $scope.changeTab($scope.servicename)
        }
    }

    
    $scope.changeTab = function(tab) {
        tab = mrl.getFullName(tab)
        $scope.view_tab = tab
        $timeout(function() {
            // lame hack rzSlider recommened
            $scope.$broadcast('rzSliderForceRender')
        })
    }


    $scope.searchText = {
        // displayName: ""
    }

    _self.changeTab = $scope.changeTab
    panelsUpdated(mrl.getPanelList())
    mrl.setSearchFunction($scope.setSearchText)
    mrl.setTabsViewCtrl(this)
    mrl.subscribeToUpdates(panelsUpdated)
}
])
