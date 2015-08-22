angular.module('mrlapp.main.mrlLogger', [])
        .factory('mrlLogger', ['mrlLog', function (mrlLog) {
                return function ($delegate) {
                    var buildTimeString = function (date, format) {
                        format = format || "%h:%m:%s:%z";
                        function pad(value, isMilliSeconds) {
                            if (typeof (isMilliSeconds) === "undefined") {
                                isMilliSeconds = false;
                            }
                            if (isMilliSeconds) {
                                if (value < 10) {
                                    value = "00" + value;
                                } else if (value < 100) {
                                    value = "0" + value;
                                }
                            }
                            return(value.toString().length < 2) ? "0" + value : value;
                        }
                        return format.replace(/%([a-zA-Z])/g, function (_, fmtCode) {
                            switch (fmtCode) {
                                case "Y":
                                    return date.getFullYear();
                                case "M":
                                    return pad(date.getMonth() + 1);
                                case "d":
                                    return pad(date.getDate());
                                case "h":
                                    return pad(date.getHours());
                                case "m":
                                    return pad(date.getMinutes());
                                case "s":
                                    return pad(date.getSeconds());
                                case "z":
                                    return pad(date.getMilliseconds(), true);
                                default:
                                    throw new Error("Unsupported format code: " + fmtCode);
                            }
                        });
                    };

                    var process = function (func, args, type) {
                        var time = buildTimeString(new Date());
                        var args2 = {};
                        angular.forEach(args, function (value, key) {
                            args2[key] = value;
                        });
                        mrlLog.addLogMessage(args2, time, type);
                        args[0] = time + " " + args[0];
                        func.apply(null, args);
                    };

                    var _log = $delegate.log;
                    var _info = $delegate.info;
                    var _warn = $delegate.warn;
                    var _debug = $delegate.debug;
                    var _error = $delegate.error;
                    $delegate.log = function () {
                        process(_log, arguments, 'log');
                    };
                    $delegate.info = function () {
                        process(_info, arguments, 'info');
                    };
                    $delegate.warn = function () {
                        process(_warn, arguments, 'warn');
                    };
                    $delegate.debug = function () {
                        process(_debug, arguments, 'debug');
                    };
                    $delegate.error = function () {
                        process(_error, arguments, 'error');
                    };
                    return $delegate;
                };

            }])
        .service('mrlLog', [function () {

                var loglist = [];

                this.addLogMessage = function (args, time, type) {
                    loglist.push({
                        args: args,
                        time: time,
                        type: type
                    });
                };

                this.getLogMessages = function () {
                    return loglist;
                };

            }]);