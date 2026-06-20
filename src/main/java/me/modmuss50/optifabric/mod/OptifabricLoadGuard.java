package me.modmuss50.optifabric.mod;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class OptifabricLoadGuard implements PreLaunchEntrypoint {
	private static void log(String message) {
		System.err.println("[OptiFabric] " + message);
		System.err.flush();
	}

	@Override
	public void onPreLaunch() {
		log("Pre-launch guard reached");
		StartupLog.record("prelaunch");
		//The first class loaded cannot have any Mixins for it or extra Mixin configs added won't apply
		//They would apply by bumping the Mixin phase afterwards, but this is a much cleaner solution
		//There is good precedent as this as a solution to the problem, first found here:
		//https://github.com/ReplayMod/ReplayMod/commit/27edfcb4f3cd0eac0c7fb24e87ee3fa67324ab0a
	}
}
