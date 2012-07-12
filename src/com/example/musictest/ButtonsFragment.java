package com.example.musictest;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import com.ecoplayer.beta.R;

public class ButtonsFragment extends Fragment {
	ImageButton playButton=null;
	ImageButton previousButton=null;
	ImageButton nextButton=null;
	TextView textViewCurrentSong=null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.player_fragment, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		playButton=(ImageButton)getActivity().findViewById(R.id.button_play);
		super.onActivityCreated(savedInstanceState);
	}
	


}
