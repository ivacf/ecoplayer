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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

//Main Activity that manages different fragment to build the UI. 
public class MainActivity extends FragmentActivity {

	public static final String EXTRA_ALBUM = "com.ecoplayer.beta.EXTRA_ALBUM";
	public static final String EXTRA_FRAGMENT_ID = "com.ecoplayer.beta.EXTRA_FRAGMENT_ID";
	public static final String EXTRA_ENERGY_MODE_SAVED = "com.ecoplayer.beta.EXTRA_ENERGY_MODE_SAVED";
	public static final String PREF_KEY_ENERGYMODE = "com.ecoplayer.beta.PREF_KEY_ENERGYMODE";
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
	private InitialEnergySettings initialEnergySettings = null;
	// SharedPreferences object
	private SharedPreferences preferences = null;
	private Album album = null;

	@Override
	protected void onCreate(Bundle arg0) {
		Debug.startMethodTracing();
		super.onCreate(arg0);
		setContentView(R.layout.main);
		// Get sharedPreferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		playQueue = PlayQueue.getInstance();
		fragmentManager = getSupportFragmentManager();
		initialEnergySettings = InitialEnergySettings.getInstance();
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
					intentEnergyService.putExtra(EnergyService.EXTRA_ENERGY_MODE, initialEnergySettings);
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
		Debug.stopMethodTracing();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_energyMode:
			// Display an alert dialog to select the energyMode
			final String[] listEnergyModes = MainActivity.this.getResources().getStringArray(R.array.energymode_array);
			final String[] listEnergyModesValues = MainActivity.this.getResources().getStringArray(
					R.array.energymode_array_values);

			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setTitle(getResources().getString(R.string.energyMode_title));
			builder.setItems(listEnergyModes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					// Save the selected energy mode ID in the SharedPrefrences
					// object.
					short energyModeID = new Short(listEnergyModesValues[item]);
					SharedPreferences.Editor editor = preferences.edit();
					editor.putInt(PREF_KEY_ENERGYMODE, energyModeID);
					editor.commit();
					// Start the energy service to enable the selected energy
					// Mode
					EnergyMode em = buildEnergyMode(energyModeID);
					Intent intentEnergyService = new Intent(getApplicationContext(), EnergyService.class);
					intentEnergyService.setAction(EnergyService.ACTION_SET_ENERGY_MODE);
					intentEnergyService.putExtra(EnergyService.EXTRA_ENERGY_MODE, em);
					startService(intentEnergyService);
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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

	// Build a energyMode object given the energyModeId.
	private EnergyMode buildEnergyMode(int energyModeId) {
		EnergyMode em = new EnergyMode();
		switch (energyModeId) {
		case 0:
			/*
			 * Building energy mode: Normal; only bluetooth off.
			 */
			em.setBluetoothOn(false);
			em.setAirPlaneModeOn(initialEnergySettings.isAirPlaneModeOn());
			em.setAutoSyncOn(initialEnergySettings.isAutoSyncOn());
			em.setBluetoothOn(initialEnergySettings.isBluetoothOn());
			em.setCPUFrequency(initialEnergySettings.getCPUFrequency());
			em.setGovernor(initialEnergySettings.getGovernor());
			break;
		case 2:
			/*
			 * Building energy mode: EcoPlus; Max cpu: 200Mhz CPU, governor:
			 * Conservative, Air plane mode ON (disable all connections), Auto
			 * sync off
			 */
			em.setAirPlaneModeOn(true);
			em.setAutoSyncOn(false);
			em.setCPUFrequency(400000);
			em.setGovernor("conservative");
			break;
		default:
			/*
			 * Building energy mode: Eco; Max cpu: 800Mhz CPU, governor:
			 * Conservative, Wifi Off, Bluetooth off, Auto sync off
			 */
			em.setWifiOn(false);
			em.setBluetoothOn(false);
			em.setAutoSyncOn(false);
			em.setCPUFrequency(800000);
			em.setGovernor("conservative");

		}
		return em;
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
				final String[] listEnergyModes = MainActivity.this.getResources().getStringArray(
						R.array.energymode_array);
				// Get the name of the energy mode currently selected
				String energyModeName = listEnergyModes[preferences.getInt(PREF_KEY_ENERGYMODE, 1)];
				Toast.makeText(getApplicationContext(),
						energyModeName + " " + getResources().getString(R.string.energyMode_enabled), Toast.LENGTH_LONG)
						.show();
				// Broadcast from energy service. THe process of retrieving and
				// saving the initial energy settings has finished.
			} else if (intent.getAction().equals(EnergyService.ENERGY_STATE_GET)) {
				AppState.setEnergyStateSaved(true);
				// Show GPS message if it's enabled
				if (initialEnergySettings.isGPSOn())
					showAlertMessageGps();
				// Call the energy service to enable the selected energy mode
				EnergyMode em = buildEnergyMode(preferences.getInt(PREF_KEY_ENERGYMODE, 1));
				Intent intentEnergyService = new Intent(getApplicationContext(), EnergyService.class);
				intentEnergyService.setAction(EnergyService.ACTION_SET_ENERGY_MODE);
				intentEnergyService.putExtra(EnergyService.EXTRA_ENERGY_MODE, em);
				startService(intentEnergyService);
			}
		}
	};

}
