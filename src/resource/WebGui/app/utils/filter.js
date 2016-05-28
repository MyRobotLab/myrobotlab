angular.module('mrlapp.utils')
        //general reverse filter
        .filter('reverse', function () {
            return function (items) {
                return items.slice().reverse();
            };
        })
        //filter to sort out any panels that don't belong to that list
        .filter('panellist', function () {
            return function (items, list) {
                var newarray = [];
                angular.forEach(items, function (value, key) {
                    if (value.list == list) {
                        newarray.push(value);
                    }
                });
                return newarray;
            };
        });
