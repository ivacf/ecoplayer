package com.example.musictest;

import java.util.ArrayList;
import java.util.Collection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.ecoplayer.beta.R;

public class SongsArrayAdapter extends ArrayAdapter<Song> {

	public SongsArrayAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.item_song, null);
		}
		Song song = getItem(position);
		if (song != null) {
			TextView textViewLeftBox = (TextView) v.findViewById(R.id.textView_leftBox);
			TextView textViewSong = (TextView) v.findViewById(R.id.textView_song);
			if (textViewLeftBox != null) {
			}
			if (textViewSong != null) {
				textViewSong.setText(song.getTitle());
			}
		}
		return v;
	}

	public Collection<Song> getCollection() {
		int size = getCount();
		ArrayList<Song> list = new ArrayList<Song>(size);
		for (int i = 0; i < size; i++) {
			list.add(i, getItem(i));
		}
		return list;
	}

}
