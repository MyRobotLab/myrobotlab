/*
 simple draggable directive using html 5 draggable attribute
*/
angular.module('mrlapp.service')
.directive('draggable', function() {
	return {
		restrict: 'A',
		link: function(scope, elem, attr, ctrl) {
			elem.draggable();
		}
	};
});