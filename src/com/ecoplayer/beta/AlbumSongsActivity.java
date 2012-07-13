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
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/*This activity shows a list of songs given a album that is expected to be received inside the intent. 
 * It loads a ButtonsFragment when there are some song inside the play queue.
 * When a song is clicked it adds the whole album to the play queue and starts the MusicService. */
public class AlbumSongsActivity extends FragmentActivity {
	public static final String EXTRA_ALBUM = "extraalbum";
	private TextView textViewLeftBox = null;
	private TextView textViewAlbum = null;
	private TextView textViewArtist = null;
	private SongsArrayAdapter songsArrayAdap = null;
	private ListView listView = null;
	private ContentResolver contentResolver = null;
	private Album album = null;
	private PlayQueue playQueue = null;
	private FragmentManager fragmentManager = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		album = (Album) getIntent().getExtras().getParcelable(EXTRA_ALBUM);
		setContentView(R.layout.album_songs);
		playQueue = PlayQueue.getInstance();
		contentResolver = getContentResolver();
		listView = (ListView) findViewById(R.id.listView);
		textViewLeftBox = (TextView) findViewById(R.id.textView_leftBox);
		textViewAlbum = (TextView) findViewById(R.id.textView_album);
		textViewArtist = (TextView) findViewById(R.id.textView_artist);
		textViewAlbum.setText(album.getTitle());
		textViewArtist.setText(album.getArtist());
		songsArrayAdap = new SongsArrayAdapter(this, android.R.layout.simple_list_item_1);
		listView.setAdapter(songsArrayAdap);
		listView.setOnItemClickListener(songSelectedListener);
		fragmentManager = getSupportFragmentManager();
		loadSongsOfAlbum();

	}

	@Override
	protected void onResume() {
		super.onResume();
		addButtonsFragmentIfNotEmpty();
	}

	//Load all the songs from a given album inside the ListView. 
	private void loadSongsOfAlbum() {
		if (album != null) {
			if (contentResolver != null) {
				Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[] {
						MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID }, MediaStore.Audio.Media.ALBUM_KEY
						+ " LIKE ?", new String[] { album.getKey() }, MediaStore.Audio.Media.TITLE + " ASC");
				if (cursor == null) {
					Log.e(this.getLocalClassName(), "Error querying songs for album '" + album.getTitle()
							+ "', cursor is null");
				} else if (!cursor.moveToFirst()) {
					Log.i(this.getLocalClassName(), "There aren't songs in the device for album " + album.getTitle());
					cursor.close();
				} else {
					int songTitleColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
					int songIdColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
					do {
						String songTitle = cursor.getString(songTitleColumn);
						int id = cursor.getInt(songIdColumn);
						if (songsArrayAdap != null) {
							songsArrayAdap.add(new Song(id, songTitle, album));
						}
					} while (cursor.moveToNext());
					songsArrayAdap.notifyDataSetChanged();
					cursor.close();
				}
			}
		}
	}

	//Add the button fragment if the play queue is not empty
	private void addButtonsFragmentIfNotEmpty() {
		if (!playQueue.isEmpty()) {
			if (fragmentManager.findFragmentById(R.id.fragment_container_songs) == null) {
				FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
				ButtonsFragment fragmentButtons = new ButtonsFragment();
				fragmentTransaction.add(R.id.fragment_container_songs, fragmentButtons);
				fragmentTransaction.commit();
			}
		}
	}

	private final OnItemClickListener songSelectedListener = new OnItemClickListener() {
		// Invoked when some item of the ListView is clicked
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			if (parent.equals(listView)) {
				//Add all songs and start playing the one in the idex=position
				playQueue.addAllSongs(songsArrayAdap.getCollection(), position);
				//Start music service
				Intent intent = new Intent(AlbumSongsActivity.this, MusicService.class);
				intent.setAction(MusicService.ACTION_NEXT);
				startService(intent);
				addButtonsFragmentIfNotEmpty();
			}
		}
	};

}
