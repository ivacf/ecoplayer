package com.example.musictest;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import com.ecoplayer.beta.R;

public class ActivityList extends Activity {

	
	private ListView listView = null;
	private AlbumsArrayAdapter albumsArrayAdapter = null;
	private ContentResolver contentResolver = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_albums);
		listView = (ListView) findViewById(R.id.listView);
		albumsArrayAdapter = new AlbumsArrayAdapter(this,
				android.R.layout.simple_list_item_1);
		listView.setAdapter(albumsArrayAdapter);
		listView.setOnItemClickListener(albumSelectedListener);
		contentResolver = getContentResolver();
		loadAlbums();

	}

	private void loadAlbums() {
		if (contentResolver != null) {
			Cursor cursor = contentResolver.query(
					MediaStore.Audio.Albums.getContentUri("external"),
					new String[] { MediaStore.Audio.Albums.ARTIST,
							MediaStore.Audio.Albums.ALBUM_KEY,
							MediaStore.Audio.Albums.NUMBER_OF_SONGS,
							MediaStore.Audio.Albums.ALBUM }, null, null,
					MediaStore.Audio.Albums.ALBUM + " ASC");
			if (cursor == null) {
				Log.e(this.getLocalClassName(),
						"Error querying albums, cursor is null");
			} else if (!cursor.moveToFirst()) {
				Log.i(this.getLocalClassName(),
						"There aren't albums in the device");
			} else {
				int albumIdColumn = cursor
						.getColumnIndex(android.provider.MediaStore.Audio.Albums.ALBUM_KEY);
				int artistColumn = cursor
						.getColumnIndex(android.provider.MediaStore.Audio.Albums.ARTIST);
				int numSongsColumn = cursor
						.getColumnIndex(android.provider.MediaStore.Audio.Albums.NUMBER_OF_SONGS);
				int albumCloumn = cursor
						.getColumnIndex(android.provider.MediaStore.Audio.Albums.ALBUM);
				do {
					String albumkey = cursor.getString(albumIdColumn);
					String artist = cursor.getString(artistColumn);
					int numSongs = cursor.getInt(numSongsColumn);
					String album = cursor.getString(albumCloumn);
					if (albumsArrayAdapter != null) {
						albumsArrayAdapter.add(new Album(albumkey, album,
								artist, numSongs));
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
					Album album=albumsArrayAdapter.getItem(position);
					Intent intent=new Intent(ActivityList.this, AlbumSongsActivity.class);
					intent.putExtra(AlbumSongsActivity.EXTRA_ALBUM, album);
					startActivity(intent);

			}
		}
	};
}
