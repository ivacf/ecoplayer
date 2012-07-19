package com.ecoplayer.beta;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class PlayQueueFragment extends Fragment {

	private SongsArrayAdapter songsArrayAdap = null;
	private ListView listView = null;
	private PlayQueue playQueue = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the view for the fragment
		return inflater.inflate(R.layout.play_queue_frag, container, false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		playQueue = PlayQueue.getInstance();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listView = (ListView) getView().findViewById(R.id.listView_songsQueue);
		songsArrayAdap = new SongsArrayAdapter(getActivity(), android.R.layout.simple_list_item_1);
		listView.setAdapter(songsArrayAdap);
		listView.setOnItemClickListener(songQueueSelectedListener);
	}

	@Override
	public void onResume() {
		super.onResume();
		loadSongsOfAlbum();
	}

	// Load all the songs from a given album inside the ListView.
	private void loadSongsOfAlbum() {
		if (!playQueue.isEmpty()) {
			songsArrayAdap.clear();
			songsArrayAdap.addAll(playQueue.getCollection());
			songsArrayAdap.notifyDataSetChanged();
		}
	}

	private final OnItemClickListener songQueueSelectedListener = new OnItemClickListener() {
		// Invoked when some item of the ListView is clicked
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			if (parent.equals(listView)) {
				// Move to the song at index=position-1
				//-1 because we will tell the music service to play the next song. 
				if (playQueue.moveToSongAt(position-1)) {
					// Start music service
					Intent intent = new Intent(PlayQueueFragment.this.getActivity(), MusicService.class);
					intent.setAction(MusicService.ACTION_NEXT);
					PlayQueueFragment.this.getActivity().startService(intent);
				} else {
					Log.e(PlayQueueFragment.this.getClass().getName(), "Error moving to song in the play queue, index="
							+ position);
				}
			}
		}
	};
}
