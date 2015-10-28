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

//<editor-fold>
//return function ($log)
//            {
//                var separator = "::",
//
//                    /**
//                     * Capture the original $log functions; for use in enhancedLogFn()
//                     */
//                    _$log = (function ($log)
//                    {
//                        return {
//                            log: $log.log,
//                            info: $log.info,
//                            warn: $log.warn,
//                            debug: $log.debug,
//                            error: $log.error
//                        };
//                    })($log),
//
//                    /**
//                     * Chrome Dev tools supports color logging
//                     * @see https://developers.google.com/chrome-developer-tools/docs/console#styling_console_output_with_css
//                     */
//                    colorify = function (message, colorCSS)
//                    {
////                        var isChrome = (BrowserDetect.browser == "Chrome") || (BrowserDetect.browser == "PhantomJS") ,
////                            canColorize = isChrome && (colorCSS !== undefined);
////
////                        return canColorize ? ["%c" + message, colorCSS] : [message];
//return message;
//                    },
//
//                    /**
//                     * Partial application to pre-capture a logger function
//                     */
//                    prepareLogFn = function (logFn, className, colorCSS)
//                    {
//                        /**
//                         * Invoke the specified `logFn` with the supplant functionality...
//                         */
//                        var enhancedLogFn = function ()
//                        {
//                            try
//                            {
//                                var args = Array.prototype.slice.call(arguments),
////                                    now = DateTime.formattedNow();
//now = new Date();
//
//                                // prepend a timestamp and optional classname to the original output message
////                                args[0] = supplant("{0} - {1}{2}", [now, className, args[0]]);
//args[0] = now + " - " + className + args[0];
////                                args = colorify(supplant.apply(null, args), colorCSS);
//args = args;
//
//                                logFn.apply(null, args);
//                            }
//                            catch(error)
//                            {
//                                $log.error("LogEnhancer ERROR: " + error);
//                            }
//
//                        };
//
//                        // Only needed to support angular-mocks expectations
//                        enhancedLogFn.logs = [];
//
//                        return enhancedLogFn;
//                    },
//
//                    /**
//                     * Support to generate class-specific logger instance with classname only
//                     */
//                    getInstance = function (className, colorCSS, customSeparator)
//                    {
//                        className = (className !== undefined) ? className + (customSeparator || separator) : "";
//
//                        var instance = {
//                            log: prepareLogFn(_$log.log, className, colorCSS),
//                            info: prepareLogFn(_$log.info, className, colorCSS),
//                            warn: prepareLogFn(_$log.warn, className, colorCSS),
//                            debug: prepareLogFn(_$log.debug, className, colorCSS),
//                            error: prepareLogFn(_$log.error, className) // NO styling of ERROR messages
//                        };
//
//                        if(angular.isDefined(angular.makeTryCatch))
//                        {
//                            // Attach instance specific tryCatch() functionality...
//                            instance.tryCatch = angular.makeTryCatch(instance.error, instance);
//                        }
//
//                        return instance;
//                    };
//
//                // Add special method to AngularJS $log
//                $log.getInstance = getInstance;
//
//                return $log;
//            };
//</editor-fold>

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

//                    this.clearLogMessages = function () {
//                        loglist = [];
//                    };
            }]);