package com.ecoplayer.beta;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class MainActivity extends FragmentActivity {

	public static final String EXTRA_ALBUM = "extraalbum";
	public static final String EXTRA_FRAGMENT_ID = "fragmentid";
	public static final short FRAGMENT_ALBUMS = 344;
	public static final short FRAGMENT_SONGS_ALBUM = 345;
	public static final short FRAGMENT_PLAY_QUEUE = 346;
	//List of valid fragments ids 
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
		if (arg0 != null) {
			album = (Album) arg0.getParcelable(EXTRA_ALBUM);
			fragmentId = arg0.getShort(EXTRA_FRAGMENT_ID);
		}
		// Load the appropiate fragment depending on the ID passed in the
		// Intent.
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
			// Stop Music service when back button is pressed if music is
			// paused.
			if (!playQueue.isPlaying()) {
				Intent intent = new Intent(this, MusicService.class);
				stopService(intent);
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
	}

}
