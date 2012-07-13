/*
 * Author:	Ivan Carballo Fernandez (icf1e11@soton.ac.uk) 
 * Project:	EcoPlayer - Battery-friendly music player for Android (MSc project at University of Southampton)
 * Date:	13-07-2012
 * License: Copyright (C) 2012 Ivan Carballo. 
 */
package com.ecoplayer.beta;



import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

//Array Adapter for managing Albums inside a ListView
public class AlbumsArrayAdapter extends ArrayAdapter<Album>{

	public AlbumsArrayAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.item_album, null);
		}
		Album album = getItem(position);
		if (album != null) {
			//TextView  textViewLeftBox= (TextView) v.findViewById(R.id.textView_leftBox);
			TextView  textViewAlbumTitle= (TextView) v.findViewById(R.id.textView_album);
			TextView textViewArtist = (TextView) v.findViewById(R.id.textView_artist);

			if (textViewAlbumTitle != null) {
				textViewAlbumTitle.setText(album.getTitle());
			}

			if (textViewArtist != null) {
				textViewArtist.setText(album.getArtist());
			}
		}
		return v;
	}

}
