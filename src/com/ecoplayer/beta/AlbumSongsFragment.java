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
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

/*This fragment shows a list of songs given a album that is expected to be set before adding the fragment. 
 * When a song is clicked it adds the whole album to the play queue and starts the MusicService. */
public class AlbumSongsFragment extends Fragment {

	private SongsArrayAdapter songsArrayAdap = null;
	private ListView listView = null;
	private ContentResolver contentResolver = null;
	private Album album = null;
	private PlayQueue playQueue = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the view for the fragment
		return inflater.inflate(R.layout.songs_album_frag, container, false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		playQueue = PlayQueue.getInstance();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listView = (ListView) getView().findViewById(R.id.listView_songs);
		songsArrayAdap = new SongsArrayAdapter(getActivity(), android.R.layout.simple_list_item_1);
		listView.setAdapter(songsArrayAdap);
		listView.setOnItemClickListener(songSelectedListener);
		contentResolver = getActivity().getContentResolver();
	}

	void setAlbum(Album album) {
		this.album = album;
	}

	@Override
	public void onResume() {
		super.onResume();
		loadSongsOfAlbum();
	}

	// Load all the songs from a given album inside the ListView.
	private void loadSongsOfAlbum() {
		if (album != null) {
			if (contentResolver != null) {
				if (songsArrayAdap != null)
					songsArrayAdap.clear();
				Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[] {
						MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID }, MediaStore.Audio.Media.ALBUM_KEY
						+ " LIKE ?", new String[] { album.getKey() }, MediaStore.Audio.Media.TITLE + " ASC");
				if (cursor == null) {
					Log.e(getActivity().getLocalClassName(), "Error querying songs for album '" + album.getTitle()
							+ "', cursor is null");
				} else if (!cursor.moveToFirst()) {
					Log.i(getActivity().getLocalClassName(),
							"There aren't songs in the device for album " + album.getTitle());
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
		} else {
			Log.e(getActivity().getLocalClassName(), "The album object is null");
		}
	}

	private final OnItemClickListener songSelectedListener = new OnItemClickListener() {
		// Invoked when some item of the ListView is clicked
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			if (parent.equals(listView)) {
				// Add all songs and start playing the one in the idex=position
				playQueue.addAllSongs(songsArrayAdap.getCollection(), position);
				// Start music service
				Intent intent = new Intent(AlbumSongsFragment.this.getActivity(), MusicService.class);
				intent.setAction(MusicService.ACTION_NEXT);
				MainActivity mainActivity = (MainActivity) AlbumSongsFragment.this.getActivity();
				mainActivity.startService(intent);
				mainActivity.addButtonsFragmentIfNotEmpty();
			}
		}
	};

}
