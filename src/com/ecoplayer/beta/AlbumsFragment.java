/*
 * Author:	Ivan Carballo Fernandez (icf1e11@soton.ac.uk) 
 * Project:	EcoPlayer - Battery-friendly music player for Android (MSc project at University of Southampton)
 * Date:	13-07-2012
 * License: Copyright (C) 2012 Ivan Carballo. 
 */
package com.ecoplayer.beta;

import android.content.ContentResolver;
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

/*This Fragment shows a list of albums and artists available to play. */
public class AlbumsFragment extends Fragment implements FragmentEcoPlayer {

	private ListView listView = null;
	private AlbumsArrayAdapter albumsArrayAdapter = null;
	private ContentResolver contentResolver = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the view for the fragment
		return inflater.inflate(R.layout.albums_frag, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listView = (ListView) getView().findViewById(R.id.listView_albums);
		albumsArrayAdapter = new AlbumsArrayAdapter(getActivity(), android.R.layout.simple_list_item_1);
		listView.setAdapter(albumsArrayAdapter);
		listView.setOnItemClickListener(albumSelectedListener);
		contentResolver = getActivity().getContentResolver();
	}

	@Override
	public void onResume() {
		super.onResume();
		loadAlbums();
	}

	// Load all the available albums by using a ContentResolver
	private void loadAlbums() {
		if (contentResolver != null) {
			if (albumsArrayAdapter != null)
				albumsArrayAdapter.clear();
			Cursor cursor = contentResolver.query(MediaStore.Audio.Albums.getContentUri("external"), new String[] {
					MediaStore.Audio.Albums.ARTIST, MediaStore.Audio.Albums.ALBUM_KEY,
					MediaStore.Audio.Albums.NUMBER_OF_SONGS, MediaStore.Audio.Albums.ALBUM }, null, null,
					MediaStore.Audio.Albums.ALBUM + " ASC");
			if (cursor == null) {
				Log.e(getActivity().getLocalClassName(), "Error querying albums, cursor is null");
			} else if (!cursor.moveToFirst()) {
				Log.i(getActivity().getLocalClassName(), "There aren't albums in the device");
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

	private final OnItemClickListener albumSelectedListener = new OnItemClickListener() {
		// Invoked when some item of the ListView is clicked
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			if (parent.equals(listView)) {
				Album album = (Album) listView.getAdapter().getItem(position);
				MainActivity mainActivity = (MainActivity) getActivity();
				mainActivity.addSongsByAlbumFragment(album);
			}
		}
	};

	@Override
	public void onSongChanged() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMusicPlayerStateChanged() {
		// TODO Auto-generated method stub
		
	}

}
