package com.example.musictest;


import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import com.ecoplayer.beta.R;

public class MainActivity extends Activity {
	TextView textView;
	Button stopButton;
	MediaPlayer mediaPlayer;
	Uri myUri;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		textView = (TextView) findViewById(R.id.textView);
		stopButton = (Button) findViewById(R.id.stopButton);
		stopButton.setOnClickListener(ocl);
		Intent intent = new Intent(this, MusicService.class);
		intent.setAction(MusicService.ACTION_PLAY);
		if (!isMyServiceRunning()) {
			startService(intent);
		}
		textView.setText("Playing...");
	}

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("com.example.musictest.MusicService".equals(service.service
					.getClassName())) {
				return true;
			}
		}
		return false;
	}

	OnClickListener ocl = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(MainActivity.this, MusicService.class);
			stopService(intent);
			textView.setText("stopped");
		}
	};

}
