package com.example.musictest;

import java.io.IOException;

import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import com.ecoplayer.beta.R;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener {
	public static final String ACTION_PLAY = "com.example.musictest.PLAY";
	public static final String ACTION_PAUSE = "com.example.musictest.PAUSE";
	public static final String ACTION_NEXT = "com.example.musictest.NEXT";
	public static final String ACTION_PREVIOUS = "com.example.musictest.PREVIOUS";
	private static final String LOG_TAG = "MusicService";
	public static int NOTIFICATION_ID = 1337;
	private MediaPlayer mMediaPlayer = null;
	private NotificationCompat.Builder notiBuilder;
	private PlayQueue playQueue = null;
	private boolean isMediaPlayerInit = false;
	NotificationManager mNotificationManager;

	@Override
	public void onCreate() {
		playQueue = PlayQueue.getInstance();
		notiBuilder = new NotificationCompat.Builder(getApplicationContext()).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(getResources().getString(R.string.app_name)).setContentIntent(buildPendingIntent())
				.setContentText(getResources().getString(R.string.tap_to_play));
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		startForeground(NOTIFICATION_ID, notiBuilder.getNotification());
		isMediaPlayerInit = initMediaPlayer();
		super.onCreate();
	}

	private boolean initMediaPlayer() {
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		int result = audioManager.requestAudioFocus(audioFocusChangeList, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);
		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			mMediaPlayer = new MediaPlayer(); // initialize it here
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setOnCompletionListener(onCompletion);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			// prevent CPU from going to sleep
			mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
			return true;
		} else {
			Log.w(LOG_TAG, "The service hasn't got the audio focus");
		}
		return false;
	}

	private boolean playSong(Uri uri) {
		if (isMediaPlayerInit) {
			mMediaPlayer.reset();
			try {
				mMediaPlayer.setDataSource(getApplicationContext(), uri);
				// prepare async to not block main thread
				mMediaPlayer.prepareAsync();
				return true;
			} catch (IllegalArgumentException e) {
				Log.e(e.getClass().getName(), e.getMessage(), e);
			} catch (SecurityException e) {
				Log.e(e.getClass().getName(), e.getMessage(), e);
			} catch (IllegalStateException e) {
				Log.e(e.getClass().getName(), e.getMessage(), e);
			} catch (IOException e) {
				Log.e(e.getClass().getName(), e.getMessage(), e);
			}
		}
		return false;

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand(intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	private void handleCommand(Intent intent) {
		// Play next song in the queue, also use to start with the first song.
		if (intent.getAction().equals(ACTION_NEXT)) {
			Song s = playQueue.getNext();
			if (s != null) {
				if (playSong(getUriFromSong(s))) {

				}
			} else {
				Log.e(getClass().getName(), "Error retrieving next song from queue, song is null");
			}
			// Pause the song
		} else if (intent.getAction().equals(ACTION_PAUSE)) {
			if (isMediaPlayerInit) {
				if (mMediaPlayer.isPlaying()) {
					mMediaPlayer.pause();
					mNotificationManager.notify(NOTIFICATION_ID, notiBuilder.setContentIntent(buildPendingIntent())
							.setContentText(getResources().getString(R.string.paused)).getNotification());
				}
			}
			// Start the song after pausing
		} else if (intent.getAction().equals(ACTION_PLAY)) {
			if (isMediaPlayerInit) {
				if (mMediaPlayer.isPlaying()) {
					mMediaPlayer.start();
					mNotificationManager.notify(
							NOTIFICATION_ID,
							notiBuilder
									.setContentIntent(buildPendingIntent())
									.setContentText(
											getResources().getString(R.string.playing) + " "
													+ playQueue.getCurrent().getTitle()).getNotification());
				}
			}
			// Play the previous song in the play queue.
		} else if (intent.getAction().equals(ACTION_PREVIOUS)) {
			Song s = playQueue.getNext();
			if (s != null) {
				playSong(getUriFromSong(s));
			} else {
				Log.e(getClass().getName(), "Error retrieving previous song from queue, song is null");
			}
		}
	}

	public Uri getUriFromSong(Song song) {
		return ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		if (mMediaPlayer.isPlaying())
			mMediaPlayer.stop();
		mMediaPlayer.release();
		mMediaPlayer = null;
		super.onDestroy();
	}

	/** Called when MediaPlayer is ready */
	public void onPrepared(MediaPlayer player) {
		player.start();
		mNotificationManager.notify(
				NOTIFICATION_ID,
				notiBuilder
						.setContentIntent(buildPendingIntent())
						.setContentText(
								getResources().getString(R.string.playing) + " " + playQueue.getCurrent().getTitle())
						.getNotification());
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	OnCompletionListener onCompletion = new OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			Song s = playQueue.getNext();
			if (s != null) {
				playSong(getUriFromSong(s));
			} else {
				Log.e(getClass().getName(), "Error retrieving next song from queue, song is null");
			}
		}
	};

	OnAudioFocusChangeListener audioFocusChangeList = new OnAudioFocusChangeListener() {

		@Override
		public void onAudioFocusChange(int focusChange) {
			switch (focusChange) {
			case AudioManager.AUDIOFOCUS_GAIN:
				// resume playback
				if (mMediaPlayer == null)
					initMediaPlayer();
				else if (!mMediaPlayer.isPlaying())
					mMediaPlayer.start();
				mMediaPlayer.setVolume(1.0f, 1.0f);
				break;

			case AudioManager.AUDIOFOCUS_LOSS:
				// Lost focus for an unbounded amount of time: stop playback and
				// release media player
				if (mMediaPlayer.isPlaying())
					mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				// Lost focus for a short time, but we have to stop
				// playback. We don't release the media player because playback
				// is likely to resume
				if (mMediaPlayer.isPlaying())
					mMediaPlayer.pause();
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				// Lost focus for a short time, but it's ok to keep playing
				// at an attenuated level
				if (mMediaPlayer.isPlaying())
					mMediaPlayer.setVolume(0.1f, 0.1f);
				break;
			}

		}
	};

	private PendingIntent buildPendingIntent() {
		Intent intent = null;
		if (isMediaPlayerInit) {
			if (mMediaPlayer.isPlaying()) {
				intent = new Intent(getApplicationContext(), AlbumSongsActivity.class);
				intent.putExtra(AlbumSongsActivity.EXTRA_ALBUM, playQueue.getCurrent().getAlbum());
			} else {
				intent = new Intent(getApplicationContext(), ActivityList.class);
			}
		} else {
			intent = new Intent(getApplicationContext(), ActivityList.class);
		}
		return PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}
}