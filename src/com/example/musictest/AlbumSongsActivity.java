package com.example.musictest;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import com.ecoplayer.beta.R;

public class AlbumSongsActivity extends FragmentActivity {
	public static final String EXTRA_ALBUM="extraalbum";
	TextView textViewLeftBox = null;
	TextView textViewAlbum = null;
	TextView textViewArtist = null;
	SongsArrayAdapter songsArrayAdap = null;
	ListView listView = null;
	ContentResolver contentResolver = null;
	Album album = null;
	PlayQueue playQueue=null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		album = (Album) getIntent().getExtras().getParcelable(
				EXTRA_ALBUM);
		setContentView(R.layout.album_songs);
		playQueue=PlayQueue.getInstance();
		contentResolver = getContentResolver();
		listView = (ListView) findViewById(R.id.listView);
		textViewLeftBox = (TextView) findViewById(R.id.textView_leftBox);
		textViewAlbum = (TextView) findViewById(R.id.textView_album);
		textViewArtist = (TextView) findViewById(R.id.textView_artist);
		textViewAlbum.setText(album.getTitle());
		textViewArtist.setText(album.getArtist());
		songsArrayAdap = new SongsArrayAdapter(this,
				android.R.layout.simple_list_item_1);
		listView.setAdapter(songsArrayAdap);
		listView.setOnItemClickListener(songSelectedListener);
		loadSongsOfAlbum();
		
	}

	private void loadSongsOfAlbum() {
		if (album != null) {
			if (contentResolver != null) {
				Cursor cursor = contentResolver.query(
						MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
						new String[] { MediaStore.Audio.Media.TITLE,
								MediaStore.Audio.Media._ID },
						MediaStore.Audio.Media.ALBUM_KEY + " LIKE ?",
						new String[] { album.getKey() },
						MediaStore.Audio.Media.TITLE + " ASC");
				if (cursor == null) {
					Log.e(this.getLocalClassName(),
							"Error querying songs for album '"
									+ album.getTitle() + "', cursor is null");
				} else if (!cursor.moveToFirst()) {
					Log.i(this.getLocalClassName(),
							"There aren't songs in the device for album "
									+ album.getTitle());
					cursor.close();
				} else {
					int songTitleColumn = cursor
							.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
					int songIdColumn = cursor
							.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
					do {
						String songTitle = cursor.getString(songTitleColumn);
						int id = cursor.getInt(songIdColumn);
						if (songsArrayAdap != null) {
							songsArrayAdap.add(new Song(id, songTitle,album));
						}
					} while (cursor.moveToNext());
					songsArrayAdap.notifyDataSetChanged();
					cursor.close();
				}
			}
		}
	}
	private final OnItemClickListener songSelectedListener = new OnItemClickListener() {
		// Invoked when some item of the ListView is clicked
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			if (parent.equals(listView)) {
					playQueue.addAllSongs(songsArrayAdap.getCollection(),position);
					Intent intent = new Intent(AlbumSongsActivity.this, MusicService.class);
					intent.setAction(MusicService.ACTION_NEXT);
					startService(intent);
			}
		}
	};
}
