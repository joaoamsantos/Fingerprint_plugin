// Empty constructor
function FingerprintPlugin() {}

FingerprintPlugin.prototype.isDevicesHasPermission = function(opts, successCallback, errorCallback) {
    alert('Entering the isDevicesHasPermission action!');
    cordova.exec( successCallback, errorCallback,'FingerprintPlugin','isDevicesHasPermission', [{'opts': opts}]);
    alert('After the isDevicesHasPermission action!');
}

FingerprintPlugin.install = function() {
	if (!window.plugins) {
		window.plugins = {};
    }
    window.plugins.FingerprintPlugin = new FingerprintPlugin();
    return window.plugins.FingerprintPlugin;
}

cordova.addConstructor(FingerprintPlugin.install);
