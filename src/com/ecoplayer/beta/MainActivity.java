/*
 * Author:	Ivan Carballo Fernandez (icf1e11@soton.ac.uk) 
 * Project:	EcoPlayer - Battery-friendly music player for Android (MSc project at University of Southampton)
 * Date:	13-07-2012
 * License: Copyright (C) 2012 Ivan Carballo. 
 */
package com.ecoplayer.beta;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

//Main Activity that manages different fragment to build the UI. 
public class MainActivity extends FragmentActivity {

	public static final String EXTRA_ALBUM = "com.ecoplayer.beta.EXTRA_ALBUM";
	public static final String EXTRA_FRAGMENT_ID = "com.ecoplayer.beta.EXTRA_FRAGMENT_ID";
	public static final String EXTRA_ENERGY_MODE_SAVED = "com.ecoplayer.beta.EXTRA_ENERGY_MODE_SAVED";
	public static final short FRAGMENT_ALBUMS = 344;
	public static final short FRAGMENT_SONGS_ALBUM = 345;
	public static final short FRAGMENT_PLAY_QUEUE = 346;
	// List of valid fragments IDs
	public static final short[] listFragmentIds = { FRAGMENT_ALBUMS, FRAGMENT_SONGS_ALBUM, FRAGMENT_PLAY_QUEUE };
	private short currentFragment = FRAGMENT_ALBUMS;
	private PlayQueue playQueue = null;
	private FragmentManager fragmentManager = null;
	private AlbumsFragment fragmentAlbums = null;
	private ButtonsFragment fragmentButtons = null;
	private AlbumSongsFragment fragmentSongsAlbum = null;
	private PlayQueueFragment fragmentPlayQueue = null;
	private Album album = null;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.main);
		playQueue = PlayQueue.getInstance();
		fragmentManager = getSupportFragmentManager();
		short fragmentId = FRAGMENT_ALBUMS;
		// Register broadcast
		IntentFilter intentFilter = new IntentFilter(MusicService.MUSIC_UPDATE);
		intentFilter.addAction(EnergyService.ENERGY_MODE_SET);
		intentFilter.addAction(EnergyService.ENERGY_STATE_GET);
		registerReceiver(broadcastReceiver, intentFilter);
		if (arg0 != null) {
			album = (Album) arg0.getParcelable(EXTRA_ALBUM);
			fragmentId = arg0.getShort(EXTRA_FRAGMENT_ID);
		}
		// Start energy service to save initial energy settings
		if (!AppState.isEnergyStateSaved()) {
			Intent intentEnergyService = new Intent(getApplicationContext(), EnergyService.class);
			intentEnergyService.setAction(EnergyService.ACTION_GET_ENERGY_STATE);
			startService(intentEnergyService);
		}
		// Load a fragment depending on the ID sent in the Intent.
		if (isFragmentIdValid(fragmentId)) {
			switch (fragmentId) {
			case FRAGMENT_ALBUMS:
				addAlbumFragment();
				break;
			case FRAGMENT_SONGS_ALBUM:
				if (album != null) {
					addSongsByAlbumFragment(album);
					break;
				}
				// if null won't break and will load the default one.
			case FRAGMENT_PLAY_QUEUE:
				addPlayQueueFragment();
				break;
			default:
				addAlbumFragment();
			}
		}
	}

	// Return true if the id is valid
	public static final boolean isFragmentIdValid(short id) {
		for (int i = 0; i < listFragmentIds.length; i++) {
			if (listFragmentIds[i] == id) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (album != null)
			outState.putParcelable(EXTRA_ALBUM, album);
		outState.putShort(EXTRA_FRAGMENT_ID, currentFragment);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed() {
		// If the current fragment is the list of albums we stop the service
		// and close the activity
		if (currentFragment == FRAGMENT_ALBUMS) {
			// Close music player when back button is pressed if music is
			// paused.
			if (!playQueue.isPlaying()) {
				Intent intent = new Intent(this, MusicService.class);
				stopService(intent);
				// Call the energy service to reset energy settings to the
				// initial ones before closing
				if (AppState.isEnergyStateSaved() && AppState.isEnergyModeEabled()) {
					Intent intentEnergyService = new Intent(getApplicationContext(), EnergyService.class);
					intentEnergyService.setAction(EnergyService.ACTION_SET_ENERGY_MODE);
					intentEnergyService.putExtra(EnergyService.EXTRA_ENERGY_MODE, InitialEnergySettings.getInstance());
					startService(intentEnergyService);
				}
				AppState.reset();
			}
			super.onBackPressed();// Close the activity
		} else {
			// If not, back to the list of albums
			addAlbumFragment();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		addButtonsFragmentIfNotEmpty();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(broadcastReceiver);
		super.onDestroy();
	}

	// Add the new fragment with the play,next and previous buttons, only if the
	// play queue isn't empty
	void addButtonsFragmentIfNotEmpty() {
		if (!playQueue.isEmpty()) {
			// Check if it's already added
			if (fragmentManager.findFragmentById(R.id.fragment_container_buttons) == null) {
				FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
				if (fragmentButtons == null)
					fragmentButtons = new ButtonsFragment();
				fragmentTransaction.add(R.id.fragment_container_buttons, fragmentButtons);
				fragmentTransaction.commit();
			}
		}
	}

	// Add a fragment that display all the albums inside the device memory
	private void addAlbumFragment() {
		currentFragment = FRAGMENT_ALBUMS;
		album = null;
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if (fragmentAlbums == null)
			fragmentAlbums = new AlbumsFragment();
		fragmentTransaction.replace(R.id.fragment_container_lists, fragmentAlbums);
		fragmentTransaction.commit();
		setTitle(getResources().getString(R.string.app_name) + " - " + getResources().getString(R.string.albums));

	}

	// Add a fragment that displays a list of songs of a given album
	void addSongsByAlbumFragment(Album album) {
		currentFragment = FRAGMENT_SONGS_ALBUM;
		this.album = album;
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if (fragmentSongsAlbum == null)
			fragmentSongsAlbum = new AlbumSongsFragment();
		fragmentSongsAlbum.setAlbum(album);
		fragmentTransaction.replace(R.id.fragment_container_lists, fragmentSongsAlbum);
		fragmentTransaction.commit();
		setTitle(album.getTitle() + " - " + album.getArtist());
	}

	// Add a fragment that displays the songs of the play queue
	void addPlayQueueFragment() {
		currentFragment = FRAGMENT_PLAY_QUEUE;
		album = null;
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if (fragmentPlayQueue == null)
			fragmentPlayQueue = new PlayQueueFragment();
		fragmentTransaction.replace(R.id.fragment_container_lists, fragmentPlayQueue);
		fragmentTransaction.commit();
		setTitle(getResources().getString(R.string.play_queue));
	}

	// Display an alert dialog to inform when GPS is enabled and gives an option
	// to disable it manually
	private void showAlertMessageGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getResources().getString(R.string.gps_message)).setCancelable(false)
				.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
					}
				}).setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						dialog.cancel();
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
	}

	// BroadcastReceiver for managing messages from the Music Service and Energy
	// service
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		public void onReceive(android.content.Context context, Intent intent) {
			// Broadcast from music service. Current song and/or player status
			// have changed
			if (intent.getAction().equals(MusicService.MUSIC_UPDATE)) {
				boolean songChanged = intent.getBooleanExtra(MusicService.SONG_CHANGED, false);
				boolean playerStateChanged = intent.getBooleanExtra(MusicService.PLAYER_STATE_CHANGED, false);
				FragmentEcoPlayer fragmentButtons = (FragmentEcoPlayer) fragmentManager
						.findFragmentById(R.id.fragment_container_buttons);
				FragmentEcoPlayer fragmentList = (FragmentEcoPlayer) fragmentManager
						.findFragmentById(R.id.fragment_container_lists);
				if (songChanged) {
					if (fragmentButtons != null)
						fragmentButtons.onSongChanged();
					if (fragmentList != null)
						fragmentList.onSongChanged();
				}
				if (playerStateChanged) {
					if (fragmentButtons != null)
						fragmentButtons.onMusicPlayerStateChanged();
					if (fragmentList != null)
						fragmentList.onMusicPlayerStateChanged();
				}
				// Broadcast from energy service. The process of enabling an
				// energy mode has finished
			} else if (intent.getAction().equals(EnergyService.ENERGY_MODE_SET)) {
				AppState.setEnergyModeEabled(true);
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.energyMode_enabled),
						Toast.LENGTH_LONG).show();
				// Broadcast from energy service. THe process of retrieving and
				// saving the initial energy settings has finished.
			} else if (intent.getAction().equals(EnergyService.ENERGY_STATE_GET)) {
				AppState.setEnergyStateSaved(true);
				// Show GPS message if it's enabled
				if (InitialEnergySettings.getInstance().isGPSOn())
					showAlertMessageGps();
				// Set the default energy mode.
				Intent intentEnergyService = new Intent(getApplicationContext(), EnergyService.class);
				intentEnergyService.setAction(EnergyService.ACTION_SET_ENERGY_MODE);
				EnergyMode em = new EnergyMode();
				em.setCPUFrequency(800000);
				em.setGovernor("conservative");
				intentEnergyService.putExtra(EnergyService.EXTRA_ENERGY_MODE, em);
				startService(intentEnergyService);
			}
		}
	};

}
