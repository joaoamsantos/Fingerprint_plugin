// Empty constructor
function FingerprintPlugin() {}

FingerprintPlugin.prototype.requestPermission = function(opts, successCallback, errorCallback) {
    alert('Entering the request Permission action!');
    cordova.exec( successCallback,
        errorCallback,
        'FingerprintPlugin',
        'requestPermission',
        [{'opts': opts}]
    );
    alert('After the request Permission action!');
}

FingerprintPlugin.install = function() {
	if (!window.plugins) {
		window.plugins = {};
    }
    window.plugins.FingerprintPlugin = new FingerprintPlugin();
    return window.plugins.FingerprintPlugin;
};

cordova.addConstructor(FingerprintPlugin.install);
