var FingerprintPlugin = {
    isDevicesHasPermission: function(opts, successCallback, errorCallback) {
        if (typeof opts === 'function') {  //user did not pass opts
          errorCallback = successCallback;
          successCallback = opts;
          opts = {};
        }
        cordova.exec(
            successCallback,
            errorCallback,
            'FingerprintPlugin',
            'isDevicesHasPermission',
            [{'opts': opts}]
        );
    },
    requestPermission: function(opts, successCallback, errorCallback) {
        if (typeof opts === 'function') {  //user did not pass opts
          errorCallback = successCallback;
          successCallback = opts;
          opts = {};
        }
        cordova.exec(
            successCallback,
            errorCallback,
            'FingerprintPlugin',
            'requestPermission',
            [{'opts': opts}]
        );
    },
    open: function(opts, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'FingerprintPlugin',
            'openSerial',
            [{'opts': opts}]
        );
    },
    read: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'FingerprintPlugin',
            'readSerial',
            []
        );
    },
    close: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'FingerprintPlugin',
            'closeSerial',
            []
        );
    },
    registerReadCallback: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'FingerprintPlugin',
            'registerReadCallback',
            []
        );
    }
};
module.exports = FingerprintPlugin;
