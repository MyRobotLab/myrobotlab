angular.module('mrlapp.mrl').controller('tabsViewCtrl', ['$location','$scope', '$log', '$filter', '$timeout', 'mrl', '$state', '$stateParams', function($location, $scope, $log, $filter, $timeout, mrl, $state, $stateParams) {
    $log.info('tabsViewCtrl $scope.$id - ' + $scope.$id)
    _self = this

    $scope.view_tab = null
    // $scope.viewType = mrl.getViewType()
    $scope.servicename = $stateParams.servicename
    $scope.mrl = mrl

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
          //  $scope.changeTab($scope.panels[0].name)
        }

        // if /#/service/{servicename} - change the tab
        if ($scope.servicename) {
          //   $scope.changeTab($scope.servicename)
        }
    }

    $scope.changeTab = function(tab) {
        tab = mrl.getFullName(tab)
        $scope.view_tab = tab
        $timeout(function() {
            // lame hack rzSlider recommened
            $scope.$broadcast('rzSliderForceRender')
        })
        var newId = "s1"
        // $location.path('service/' + tab, false)
        // $state.go('tabs2','/service/' + tab)

        $state.transitionTo('tabs2', {id: tab}, {
            location: true,
            inherit: true,
            relative: $state.$current,
            notify: false
        })

        $state.go('tabs2', { servicename: tab }, {notify:false, reload:true})
        // $state.go($state.current, {}, {reload: true})
        /*
        $state.transitionTo('tabs2', {
            id: newId
        }, {
            location: true,
            inherit: true,
            relative: $state.$current,
            notify: false
        })*/
    }

    $scope.searchText = {// displayName: ""
    }

    $scope.hasNewStatus = true

    _self.changeTab = $scope.changeTab
    panelsUpdated(mrl.getPanelList())
    mrl.setSearchFunction($scope.setSearchText)
    mrl.setTabsViewCtrl(this)
    mrl.subscribeToUpdates(panelsUpdated)
}
])
