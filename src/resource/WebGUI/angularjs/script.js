(function(){

	var app = angular.module("routedTabs", ["ui.router", "ui.bootstrap"]);

	app.config(function($stateProvider, $urlRouterProvider){

		$urlRouterProvider.otherwise("/main/tab1");

		$stateProvider
			.state("main", { abtract: true, url:"/main", templateUrl:"main.html" })
				.state("main.tab1", { url: "/tab1", templateUrl: "tab1.html" })
				.state("main.tab2", { url: "/tab2", templateUrl: "tab2.html" })
				.state("main.tab3", { url: "/tab3", templateUrl: "tab3.html" });

	});

	app.controller("mainController", function($rootScope, $scope, $state) {		

		$scope.go = function(route){
			$state.go(route);
		};

		$scope.active = function(route){
			return $state.is(route);
		};

		$scope.tabs = [
			{ heading: "Tab 1", route:"main.tab1", active:false },
			{ heading: "Tab 2", route:"main.tab2", active:false },
			{ heading: "Tab 3", route:"main.tab3", active:false },
		];

		$scope.$on("$stateChangeSuccess", function() {
			$scope.tabs.forEach(function(tab) {
				tab.active = $scope.active(tab.route);
			});
		});
	});

}());
