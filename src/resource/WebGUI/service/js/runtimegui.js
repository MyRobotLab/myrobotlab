angular.module('mrlapp.service.runtimegui', [])

.controller(
		'RuntimeGuiCtrl',
		[ '$scope', 'mrl',
				function($scope, mrl) {
					var myService = mrl.services["runtime"];
					console.log(myService);
					console.log('here');

				} ]);