function TimeDurationDirective() {
    var tpl = "<div> \
     <input type='text' ng-model='num' size='80' /> \
     <select ng-model='unit'> \
         <option value='seconds'>Seconds</option> \
         <option value='minutes'>Minutes</option> \
         <option value='hours'>Hours</option> \
         <option value='days'>Days</option> \
     </select> \
 </div>";
    return {
        restrict: 'E',
        template: tpl,
        require: 'ngModel',
        // replace: true, - deprecated
        link: function(scope, iElement, iAttrs, ngModelCtrl) {
            // Units of time
            var multiplierMap = {
                seconds: 1,
                minutes: 60,
                hours: 3600,
                days: 86400
            };
            var multiplierTypes = ['seconds', 'minutes', 'hours', 'days']
            ngModelCtrl.$formatters.push(function(modelValue) {
                var unit = 'minutes', num = 0, i, unitName;
                modelValue = parseInt(modelValue || 0);
                // Figure out the largest unit of time the model value
                // fits into. For example, 3600 is 1 hour, but 1800 is 30 minutes.
                for (i = multiplierTypes.length - 1; i >= 0; i--) {
                    unitName = multiplierTypes[i];
                    if (modelValue % multiplierMap[unitName] === 0) {
                        unit = unitName;
                        break;
                    }
                }
                if (modelValue) {
                    num = modelValue / multiplierMap[unit]
                }
                return {
                    unit: unit,
                    num: num
                };
            });
            ngModelCtrl.$parsers.push(function(viewValue) {
                var unit = viewValue.unit, num = viewValue.num, multiplier;
                multiplier = multiplierMap[unit];
                return num * multiplier;
            });
            scope.$watch('unit + num', function() {
                ngModelCtrl.$setViewValue({
                    unit: scope.unit,
                    num: scope.num
                });
            });
            ngModelCtrl.$render = function() {
                scope.unit = ngModelCtrl.$viewValue.unit;
                scope.num = ngModelCtrl.$viewValue.num;
            }
            ;
        }
    };
}
;angular.module('mrlapp.service').directive('timeDuration', TimeDurationDirective);
