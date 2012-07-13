/*
 * Author:	Ivan Carballo Fernandez (icf1e11@soton.ac.uk) 
 * Project:	EcoPlayer - Battery-friendly music player for Android (MSc project at University of Southampton)
 * Date:	13-07-2012
 * License: Copyright (C) 2012 Ivan Carballo. 
 */
package com.ecoplayer.beta;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

/*This activity shows a list of albums and artists available to play.
 * It loads a ButtonsFragment when there are some song inside the play queue.
 * When one album is clicked it starts AlbumSongsActivty with the Album object inside the Intent */
public class ActivityAlbums extends FragmentActivity {

	private ListView listView = null;
	private AlbumsArrayAdapter albumsArrayAdapter = null;
	private ContentResolver contentResolver = null;
	private PlayQueue playQueue = null;
	private ButtonsFragment fragmentButtons = null;
	private FragmentManager fragmentManager = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_albums);
		listView = (ListView) findViewById(R.id.listView);
		albumsArrayAdapter = new AlbumsArrayAdapter(this, android.R.layout.simple_list_item_1);
		listView.setAdapter(albumsArrayAdapter);
		listView.setOnItemClickListener(albumSelectedListener);
		playQueue = PlayQueue.getInstance();
		contentResolver = getContentResolver();
		fragmentManager = getSupportFragmentManager();
		loadAlbums();

	}

	@Override
	protected void onResume() {
		super.onResume();
		addButtonsFragmentIfNotEmpty();

	}

	//Load all the available albums by using a ContentResolver 
	private void loadAlbums() {
		if (contentResolver != null) {
			Cursor cursor = contentResolver.query(MediaStore.Audio.Albums.getContentUri("external"), new String[] {
					MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM_KEY,
					MediaStore.Audio.Albums.NUMBER_OF_SONGS, MediaStore.Audio.Albums.ALBUM }, null, null,
					MediaStore.Audio.Albums.ALBUM + " ASC");
			if (cursor == null) {
				Log.e(this.getLocalClassName(), "Error querying albums, cursor is null");
			} else if (!cursor.moveToFirst()) {
				Log.i(this.getLocalClassName(), "There aren't albums in the device");
			} else {
				int albumIdColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ALBUM_KEY);
				int artistColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ARTIST);
				int numSongsColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.NUMBER_OF_SONGS);
				int albumCloumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ALBUM);
				do {
					String albumkey = cursor.getString(albumIdColumn);
					String artist = cursor.getString(artistColumn);
					int numSongs = cursor.getInt(numSongsColumn);
					String album = cursor.getString(albumCloumn);
					if (albumsArrayAdapter != null) {
						albumsArrayAdapter.add(new Album(albumkey, album, artist, numSongs));
					}
				} while (cursor.moveToNext());
				albumsArrayAdapter.notifyDataSetChanged();
				cursor.close();
			}

		}
	}

	//Add the new fragment with the play,next and previous buttons, only if the play queue isn't empty
	private void addButtonsFragmentIfNotEmpty() {
		if (!playQueue.isEmpty()) {
			//Check if it's already added 
			if (fragmentManager.findFragmentById(R.id.fragment_container) == null) {
				FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
				fragmentButtons = new ButtonsFragment();
				fragmentTransaction.add(R.id.fragment_container, fragmentButtons);
				fragmentTransaction.commit();
			}
		}
	}

	@Override
	public void onBackPressed() {
		//Stop Music service when back button is pressed. 
		if (!playQueue.isPlaying()) {
			Intent intent = new Intent(this, MusicService.class);
			stopService(intent);
		}
		super.onBackPressed();
	}

	private final OnItemClickListener albumSelectedListener = new OnItemClickListener() {
		// Invoked when some item of the ListView is clicked
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			if (parent.equals(listView)) {
				Album album = albumsArrayAdapter.getItem(position);
				Intent intent = new Intent(ActivityAlbums.this, AlbumSongsActivity.class);
				intent.putExtra(AlbumSongsActivity.EXTRA_ALBUM, album);
				startActivity(intent);

			}
		}
	};
}
