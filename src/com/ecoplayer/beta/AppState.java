/*
 * Author:	Ivan Carballo Fernandez (icf1e11@soton.ac.uk) 
 * Project:	EcoPlayer - Battery-friendly music player for Android (MSc project at University of Southampton)
 * Date:	13-07-2012
 * License: Copyright (C) 2012 Ivan Carballo. 
 */
package com.ecoplayer.beta;

//Class used to save data/flags across the app. 
public final class AppState {

	// True if the initial energy settings has been saved
	private static boolean energyStateSaved = false;
	// True if some energy mode has been enabled
	private static boolean energyModeEabled = false;

	public static boolean isEnergyStateSaved() {
		return energyStateSaved;
	}

	public static void setEnergyStateSaved(boolean energyStateSaved) {
		AppState.energyStateSaved = energyStateSaved;
	}

	public static boolean isEnergyModeEabled() {
		return energyModeEabled;
	}

	public static void setEnergyModeEabled(boolean energyModeEabled) {
		AppState.energyModeEabled = energyModeEabled;
	}

	public static void reset() {
		energyStateSaved = false;
		energyModeEabled = false;
	}

}
