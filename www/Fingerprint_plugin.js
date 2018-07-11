// Empty constructor
function FingerprintPlugin() {}

/*var serial = {
    requestPermission: function(opts, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'Serial',
            'requestPermission',
            [{'opts': opts}]
        );
    },
    open: function(opts, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'Serial',
            'openSerial',
            [{'opts': opts}]
        );
    },
    write: function(data, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'Serial',
            'writeSerial',
            [{'data': data}]
        );
    },
    writeHex: function(hexString, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'Serial',
            'writeSerialHex',
            [{'data': hexString}]
        );
    },
    read: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'Serial',
            'readSerial',
            []
        );
    },
    close: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'Serial',
            'closeSerial',
            []
        );
    },
    registerReadCallback: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'Serial',
            'registerReadCallback',
            []
        );
    }

}; */

FingerprintPlugin.prototype.requestPermission = function(opts, successCallback, errorCallback) {
    alert('Entering the request Permission action!');
    cordova.exec( successCallback,
        errorCallback,
        'FingerprintPlugin',
        'requestPermission',
        [{'opts': opts}]
    );
    alert('After the request Permission action!');
};

FingerprintPlugin.prototype.isDevicesHasPermission = function(opts, successCallback, errorCallback) {
    alert('Entering the isDevicesHasPermission action!');
    cordova.exec( successCallback,
        errorCallback,
        'FingerprintPlugin',
        'isDevicesHasPermission',
        []
    );
    alert('After the isDevicesHasPermission action!');
};

FingerprintPlugin.install = function() {
	if (!window.plugins) {
		window.plugins = {};
    }
    window.plugins.FingerprintPlugin = new FingerprintPlugin();
    return window.plugins.FingerprintPlugin;
};

cordova.addConstructor(FingerprintPlugin.install);
