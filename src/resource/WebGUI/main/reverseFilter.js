/*
    general reverse directive
*/
angular.module('mrlapp.main.reversefilter', [])
.filter('reverse', function() {
    return function(items) {
        return items.slice().reverse();
    };
});
