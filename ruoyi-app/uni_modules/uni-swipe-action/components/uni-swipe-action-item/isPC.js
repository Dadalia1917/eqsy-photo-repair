export function isPC() {
	try {
		if (typeof uni !== 'undefined' && uni.getSystemInfoSync) {
			const platform = String((uni.getSystemInfoSync() || {}).platform || '').toLowerCase();
			return platform === 'windows' || platform === 'mac';
		}
	} catch (e) {}

	if (typeof navigator !== 'undefined') {
		const userAgentInfo = navigator.userAgent || '';
		const agents = ["Android", "iPhone", "SymbianOS", "Windows Phone", "iPad", "iPod", "HarmonyOS"];
		for (let v = 0; v < agents.length; v++) {
			if (userAgentInfo.indexOf(agents[v]) >= 0) {
				return false;
			}
		}
	}

	return true;
}
