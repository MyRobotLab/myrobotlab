angular.module('mrlapp.mrl').controller('tabsViewCtrl', ['$location', '$scope', '$filter', '$timeout', 'mrl', '$state', '$stateParams', function($location, $scope, $filter, $timeout, mrl, $state, $stateParams) {
    console.info('tabsViewCtrl $scope.$id - ' + $scope.$id)
    _self = this

    $scope.history = []

    $scope.view_tab = null
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

        if (!$scope.view_tab && panels.length > 0 && $scope.panels[$scope.panels.length - 1].name.startsWith('intro')) {//  $scope.changeTab($scope.panels[0].name)
        }

        // if /#/service/{servicename} - change the tab
        if ($scope.servicename) {//   $scope.changeTab($scope.servicename)
        }
    }

    /**
     * go to a service tab
     * direction - reverse or null (forward)
     */
    $scope.changeTab = function(tab) {
        tab = mrl.getFullName(tab)
        $scope.view_tab = tab
        $scope.history.push(tab)

        // $location.path('service/' + tab, false)
        // $state.go('tabs2','/service/' + tab)

        //         $state.transitionTo('tabs2', {id: tab}, {
        //             location: true,
        //             inherit: true,
        //             relative: $state.$current,
        //             notify: false
        //         })

        $state.go('tabs2', {
            servicename: tab
        }, {
            notify: false,
            reload: false
        })
        //        $state.go('tabs2', { servicename: tab }, {notify:false, reload:true})
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

    $scope.goBack = function(){
        // pop self
        $scope.history.pop()
        // go back one
        tab = $scope.history[$scope.history.length - 1]
        $scope.view_tab = tab

        $state.go('tabs2', {
            servicename: tab
        }, {
            notify: false,
            reload: false
        })
        
    }

    $scope.searchText = {// displayName: ""
    }

    $scope.hasNewStatus = true
    _self.changeTab = $scope.changeTab
    _self.goBack = $scope.goBack
    panelsUpdated(mrl.getPanelList())
    mrl.setSearchFunction($scope.setSearchText)
    mrl.setTabsViewCtrl(this)
    mrl.subscribeToUpdates(panelsUpdated)
}
])
