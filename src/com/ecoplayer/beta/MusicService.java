/*
 * Author:	Ivan Carballo Fernandez (icf1e11@soton.ac.uk) 
 * Project:	EcoPlayer - Battery-friendly music player for Android (MSc project at University of Southampton)
 * Date:	13-07-2012
 * License: Copyright (C) 2012 Ivan Carballo. 
 */
package com.ecoplayer.beta;

import java.io.IOException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;

//Music service for playing music from a play queue. 
//It support several commands such as start, play, next, previous, etc... 
public class MusicService extends Service implements MediaPlayer.OnPreparedListener {
	public static final String MUSIC_UPDATE = "com.ecoplayer.beta.MUSIC_UPDATE";
	public static final String SONG_CHANGED = "com.ecoplayer.beta.SONG_CHANGED";
	public static final String PLAYER_STATE_CHANGED = "com.ecoplayer.beta.PLAYER_STATE_CHANGED";
	public static final String ACTION_PLAY = "com.ecoplayer.beta.PLAY";
	public static final String ACTION_PAUSE = "com.ecoplayer.beta.PAUSE";
	public static final String ACTION_NEXT = "com.ecoplayer.beta.NEXT";
	public static final String ACTION_PREVIOUS = "com.ecoplayer.beta.PREVIOUS";
	// If the previous song button is pressed before the song has been played
	// for longer than MIN_TIME_GOING_PREVIOUS
	// the player will move to the previous song instead of starting again the
	// current one.
	public static final int MIN_TIME_GOING_PREVIOUS = 1500; // mill, so 1.5s
	private static final String LOG_TAG = "MusicService";
	public static int NOTIFICATION_ID = 1337;
	private MediaPlayer mMediaPlayer = null;
	private NotificationCompat.Builder notiBuilder;
	private PlayQueue playQueue = null;
	// True if the MediaPlayer has been initialised but not prepared
	private boolean isMediaPlayerInit = false;
	// True if the MediaPlayer is paused.
	private boolean isMediaPlayerPaused = false;
	private boolean isPausedBecauseOfButton = false;
	// Reference to the son that is been played
	private Song songPlaying = null;
	private NotificationManager mNotificationManager;
	// Cpu service profiler name
	private ComponentName cpuProfilerName = null;
	private Intent intentCpuProfilerService;

	@Override
	public void onCreate() {
		super.onCreate();
		playQueue = PlayQueue.getInstance();
		notiBuilder = new NotificationCompat.Builder(getApplicationContext()).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(getResources().getString(R.string.app_name)).setContentIntent(buildPendingIntent())
				.setContentText(getResources().getString(R.string.tap_to_play));
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		isMediaPlayerInit = initMediaPlayer();
		startCpuProfilerService();
	}

	// This method initialize the Media Player
	private boolean initMediaPlayer() {
		startForeground(NOTIFICATION_ID, notiBuilder.getNotification());
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

	// Send message to Activity, if the first parameter is true it means the
	// player has change from palying to
	// paused or vice versa. If the second is true it means that a new song is
	// starting to be payed.
	private void sendMusicUpdateToActivity(boolean playerStateChanged, boolean song_changed) {
		if (song_changed) {
			Log.d("SongBlue", "Notified song chaged, current song " + songPlaying.getTitle());
		}
		Intent intentMusicUpdate = new Intent(MUSIC_UPDATE);
		intentMusicUpdate.putExtra(PLAYER_STATE_CHANGED, playerStateChanged);
		intentMusicUpdate.putExtra(SONG_CHANGED, song_changed);
		sendBroadcast(intentMusicUpdate);
	}

	// Play a given song. This method is async.
	private boolean playSong(Song song) {
		resetSongPlayingRef();
		if (song != null) {
			if (isMediaPlayerInit) {
				mMediaPlayer.reset();
				try {
					mMediaPlayer.setDataSource(getApplicationContext(), getUriFromSong(song));
					// prepare async to not block main thread
					mMediaPlayer.prepareAsync();
					songPlaying = song;
					// Set the flag in the song to playing
					songPlaying.setPlaying(true);
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
		} else {
			Log.e(getClass().getName(), "Error retrieving next song from queue, song is null");
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

	// Handle the commands received from the Activities.
	private void handleCommand(Intent intent) {
		// Play next song in the queue, also use to start with the first song.
		if (intent.getAction().equals(ACTION_NEXT)) {
			playSong(playQueue.getNext());
			// Pause the song
		} else if (intent.getAction().equals(ACTION_PAUSE)) {
			if (isMediaPlayerInit) {
				if (mMediaPlayer.isPlaying()) {
					mMediaPlayer.pause();
					isPausedBecauseOfButton = true;
					onPause();
				}
			}
			// Start the song after pausing
		} else if (intent.getAction().equals(ACTION_PLAY)) {
			if (isMediaPlayerInit) {
				if (!playQueue.isEmpty()) {
					if (isMediaPlayerPaused) {
						mMediaPlayer.start();
						sendMusicUpdateToActivity(true, false);
						onPlaying();
					} else {
						playSong(playQueue.getCurrent());
					}
				}
			}
			// Play the previous song in the play queue.
		} else if (intent.getAction().equals(ACTION_PREVIOUS)) {
			if (mMediaPlayer.getCurrentPosition() <= MIN_TIME_GOING_PREVIOUS) {
				playSong(playQueue.getPrevious());
			} else {
				playSong(playQueue.getCurrent());
			}
		}
	}

	// Called after starting to play or resuming
	private void onPlaying() {
		isMediaPlayerPaused = false;
		isPausedBecauseOfButton = false;
		playQueue.setPlaying(true);
		mNotificationManager.notify(
				NOTIFICATION_ID,
				notiBuilder
						.setContentIntent(buildPendingIntent())
						.setContentText(
								getResources().getString(R.string.playing) + " " + playQueue.getCurrent().getTitle())
						.getNotification());
	}

	// Called after pausing the music player
	private void onPause() {
		isMediaPlayerPaused = true;
		playQueue.setPlaying(false);
		sendMusicUpdateToActivity(true, false);
		mNotificationManager.notify(
				NOTIFICATION_ID,
				notiBuilder.setContentIntent(buildPendingIntent())
						.setContentText(getResources().getString(R.string.paused)).getNotification());
	}

	// Returns the URI of a given Song object
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
		stopCpuProfilerService();
		super.onDestroy();
	}

	/** Called when MediaPlayer is ready */
	public void onPrepared(MediaPlayer player) {
		player.start();
		sendMusicUpdateToActivity(true, true);
		onPlaying();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	// Reset the songPlaying var to null
	private void resetSongPlayingRef() {
		if (songPlaying != null) {
			// Set flag false and set reference to the current song to null
			Log.d("SongBlue", "Reseting song " + songPlaying.getTitle());
			songPlaying.setPlaying(false);
			songPlaying = null;
		}

	}

	OnCompletionListener onCompletion = new OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			resetSongPlayingRef();
			// Play next
			playSong(playQueue.getNext());
		}
	};

	OnAudioFocusChangeListener audioFocusChangeList = new OnAudioFocusChangeListener() {

		@Override
		public void onAudioFocusChange(int focusChange) {
			switch (focusChange) {
			case AudioManager.AUDIOFOCUS_GAIN:
				// If is not paused because the pause button was pressed resume.
				if (!isPausedBecauseOfButton) {
					// resume playback
					if (mMediaPlayer == null)
						// When focus is gained for first time or is lost for a
						// long time
						isMediaPlayerInit = initMediaPlayer();
					else if (!mMediaPlayer.isPlaying()) {
						// After losing focus for a short time
						mMediaPlayer.start();
						sendMusicUpdateToActivity(true, false);
						onPlaying();
					} else {
						// Back from AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
						mMediaPlayer.setVolume(1.0f, 1.0f);
					}
				}
				break;

			case AudioManager.AUDIOFOCUS_LOSS:
				// Lost focus for an unbounded amount of time: stop playback and
				// release media player
				if (mMediaPlayer != null) {
					if (mMediaPlayer.isPlaying()) {
						mMediaPlayer.stop();
						sendMusicUpdateToActivity(true, false);
						onPause();
					}
					MusicService.this.stopForeground(true);
					mMediaPlayer.release();
					isMediaPlayerInit = false;
					mMediaPlayer = null;
				}
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				// Lost focus for a short time, but we have to stop
				// playback. We don't release the media player because playback
				// is likely to resume
				if (mMediaPlayer != null) {
					if (mMediaPlayer.isPlaying()) {
						mMediaPlayer.pause();
						sendMusicUpdateToActivity(true, false);
						onPause();
					}
				}
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				// Lost focus for a short time, but it's ok to keep playing
				// at an attenuated level
				if (mMediaPlayer != null) {
					if (mMediaPlayer.isPlaying())
						mMediaPlayer.setVolume(0.1f, 0.1f);
				}
				break;
			}

		}
	};

	private PendingIntent buildPendingIntent() {
		Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		if (isMediaPlayerInit) {
			if (mMediaPlayer.isPlaying())
				intent.putExtra(MainActivity.EXTRA_FRAGMENT_ID, MainActivity.FRAGMENT_PLAY_QUEUE);
		}
		return PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	// Starts service com.byivan.cpufrequencies.CpuProfilerService that saves
	// logs about the time in each CPU frequency. The logs are located in
	// <external_storage>/cpu_frequencies/time_in_state_logs_<date>.csv
	// It's only started if R.bool.cpu_profiling is true.
	private void startCpuProfilerService() {
		if (getResources().getBoolean(R.bool.cpu_profiling)) {
			// If serviceCpuName is null it means the service hasn't been
			// started yet.
			if (cpuProfilerName == null) {
				String action = "com.byivan.cpufrequencies.action.PROFILING_TIME_IN_STATE";
				intentCpuProfilerService = new Intent(action);
				intentCpuProfilerService.addCategory("com.byivan.cpufrequencies.category.DEFAULT");
				cpuProfilerName = startService(intentCpuProfilerService);
				if (cpuProfilerName == null)
					Log.e(getClass().getName(), "Service for action=" + action + " cannot be started");
				else
					Log.i(getClass().getName(), "Service " + cpuProfilerName.getClassName() + " has been started");
			}
		}
	}

	// Stops service com.byivan.cpufrequencies.CpuProfilerService if enabled
	private void stopCpuProfilerService() {
		if (cpuProfilerName != null && intentCpuProfilerService != null)
			stopService(intentCpuProfilerService);
	}
}