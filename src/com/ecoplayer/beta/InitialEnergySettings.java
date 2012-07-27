/*
 * Author:	Ivan Carballo Fernandez (icf1e11@soton.ac.uk) 
 * Project:	EcoPlayer - Battery-friendly music player for Android (MSc project at University of Southampton)
 * Date:	13-07-2012
 * License: Copyright (C) 2012 Ivan Carballo. 
 */
package com.ecoplayer.beta;

/*Subclass of EnergyMode that implements a singleton pattern. 
 * It's intended to save the initial energy settings so it assures the initial settings are always
 * accessible to any component in the app at any time.   
 */
public class InitialEnergySettings extends EnergyMode {

	private static final long serialVersionUID = -6360485931059763404L;
	private static InitialEnergySettings singletonRef = null;

	private InitialEnergySettings() {
		super();
	}

	public static InitialEnergySettings getInstance() {
		if (singletonRef == null) {
			singletonRef = new InitialEnergySettings();
		}
		return singletonRef;
	}

}
