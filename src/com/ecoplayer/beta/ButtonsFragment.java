/*
 * Author:	Ivan Carballo Fernandez (icf1e11@soton.ac.uk) 
 * Project:	EcoPlayer - Battery-friendly music player for Android (MSc project at University of Southampton)
 * Date:	13-07-2012
 * License: Copyright (C) 2012 Ivan Carballo. 
 */
package com.ecoplayer.beta;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

//Fragment that shows the name of the song currently playing and three buttons (previous, play, next). 
public class ButtonsFragment extends Fragment implements FragmentEcoPlayer {

	private ImageButton playButton = null;
	private ImageButton previousButton = null;
	private ImageButton nextButton = null;
	private TextView textViewCurrentSong = null;
	private Intent intent = null;
	private PlayQueue playQueue = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		intent = new Intent(getActivity(), MusicService.class);
		playQueue = PlayQueue.getInstance();

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the view for the fragment
		return inflater.inflate(R.layout.player_frag, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		playButton = (ImageButton) getView().findViewById(R.id.button_play);
		playButton.setOnClickListener(clickListener);
		updatePlayButton();
		previousButton = (ImageButton) getView().findViewById(R.id.button_previous);
		previousButton.setOnClickListener(clickListener);
		nextButton = (ImageButton) getView().findViewById(R.id.button_next);
		nextButton.setOnClickListener(clickListener);
		textViewCurrentSong = (TextView) getView().findViewById(R.id.textView_current_song);
		textViewCurrentSong.setOnClickListener(clickListener);
		// Update the current song textView
		Song song = playQueue.getCurrent();
		if (song != null) {
			textViewCurrentSong.setText(song.getTitle() + " - " + song.getAlbum().getArtist());
		}
		super.onActivityCreated(savedInstanceState);
	}

	// Click listener for the three buttons
	private OnClickListener clickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			v.setClickable(false);
			if (v.equals(textViewCurrentSong)) {
				// The textView with the name of the song is pressed so the
				// play queue fragment is shown
				MainActivity mainActivity = (MainActivity) ButtonsFragment.this.getActivity();
				if (mainActivity != null)
					mainActivity.addPlayQueueFragment();
			} else {
				if (v.equals(playButton)) {
					// The play button becomes pause button when playing
					if (playQueue.isPlaying()) {
						intent.setAction(MusicService.ACTION_PAUSE);
					} else {
						intent.setAction(MusicService.ACTION_PLAY);
					}
				} else if (v.equals(previousButton)) {
					intent.setAction(MusicService.ACTION_PREVIOUS);

				} else if (v.equals(nextButton)) {
					intent.setAction(MusicService.ACTION_NEXT);

				} else {
					return;
				}
				// Don't start the music service if the playing queue is empty
				if (!playQueue.isEmpty()) {
					getActivity().startService(intent);
				}
			}
			v.setClickable(true);

		}
	};

	// Updates the PlayButton image drawable depending on the state of the
	// player.
	private void updatePlayButton() {
		if (playQueue.isPlaying()) {
			playButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
		} else {
			playButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
		}
	}

	@Override
	// Runs when a new song is about to play
	public void onSongChanged() {
		Song song = playQueue.getCurrent();
		textViewCurrentSong.setText(song.getTitle() + " - " + song.getAlbum().getArtist());

	}

	@Override
	public void onMusicPlayerStateChanged() {
		updatePlayButton();

	}

}
