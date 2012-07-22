/*
 * Author:	Ivan Carballo Fernandez (icf1e11@soton.ac.uk) 
 * Project:	EcoPlayer - Battery-friendly music player for Android (MSc project at University of Southampton)
 * Date:	13-07-2012
 * License: Copyright (C) 2012 Ivan Carballo. 
 */
package com.ecoplayer.beta;

//Interface that should be implemented by all the fragments of the app
interface FragmentEcoPlayer {

	abstract void onSongChanged();
	abstract void onMusicPlayerStateChanged();
}
