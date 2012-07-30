/*
 * Author:	Ivan Carballo Fernandez (icf1e11@soton.ac.uk) 
 * Project:	EcoPlayer - Battery-friendly music player for Android (MSc project at University of Southampton)
 * Date:	13-07-2012
 * License: Copyright (C) 2012 Ivan Carballo. 
 */
package com.ecoplayer.beta;

import java.io.Serializable;

//This class contains a set of energy settings that can be used to represent an energy mode or to save the initial energy settings 
public class EnergyMode implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final int NO_FREQUENCY = -1;
	private boolean wifiOn = true;
	private boolean autoSyncOn = true;
	private boolean bluetoothOn = false;
	private boolean airPlaneModeOn = false;
	private int CPUFrequency = NO_FREQUENCY;
	// CPU governor
	private String governor = null;

	public boolean isWifiOn() {
		return wifiOn;
	}

	public void setWifiOn(boolean wifiOn) {
		this.wifiOn = wifiOn;
	}

	public boolean isAutoSyncOn() {
		return autoSyncOn;
	}

	public void setAutoSyncOn(boolean autoSyncOn) {
		this.autoSyncOn = autoSyncOn;
	}

	public boolean isBluetoothOn() {
		return bluetoothOn;
	}

	public void setBluetoothOn(boolean bluetoothOn) {
		this.bluetoothOn = bluetoothOn;
	}

	public boolean isAirPlaneModeOn() {
		return airPlaneModeOn;
	}

	public void setAirPlaneModeOn(boolean airPlaneModeOn) {
		this.airPlaneModeOn = airPlaneModeOn;
	}

	public int getCPUFrequency() {
		return CPUFrequency;
	}

	public void setCPUFrequency(int cPUFrequency) {
		if (cPUFrequency >= 0)
			CPUFrequency = cPUFrequency;
	}

	public String getGovernor() {
		return governor;
	}

	public void setGovernor(String governor) {
		if (governor != null) {
			if (governor.length() > 0)
				this.governor = governor;
		}
	}

	public String toString() {
		return "wifi on: " + wifiOn + "\n bluetooth on: " + bluetoothOn + "\n autosync on: " + autoSyncOn
				+ "\n airplane on: " + airPlaneModeOn + "\n max frequency: " + CPUFrequency + "\n cpu governor: "
				+ governor;
	}
}
