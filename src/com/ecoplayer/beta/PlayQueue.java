/*
 * Author:	Ivan Carballo Fernandez (icf1e11@soton.ac.uk) 
 * Project:	EcoPlayer - Battery-friendly music player for Android (MSc project at University of Southampton)
 * Date:	13-07-2012
 * License: Copyright (C) 2012 Ivan Carballo. 
 */
package com.ecoplayer.beta;

import java.util.Collection;
import java.util.Vector;

//Representation of a play queue. Implements a singleton pattern so every object in the app can access the same instance. 
public class PlayQueue {

	// Use vector because is synchronized
	private Vector<Song> playQueue = null;
	// Max number songs that can be in the play queue. Prevent from bad
	// performance when play queue is too big
	public static final int MAX_SONGS_QUEUE = 500;
	private static PlayQueue singleton;
	public static final int INITIAL_INDEX = -1;
	private int index = INITIAL_INDEX;
	private boolean isPlaying = false;

	private PlayQueue() {
		playQueue = new Vector<Song>();
	}

	public synchronized boolean isEmpty() {
		return playQueue.isEmpty();
	}

	// Singleton
	public synchronized static PlayQueue getInstance() {
		if (singleton == null) {
			singleton = new PlayQueue();
		}
		return singleton;
	}

	// Add a song to play right now and clear the queue.
	public synchronized void addSong(Song song) {
		if (song != null) {
			clear();
			playQueue.add(song);
		}
	}

	// Add a song to the end of the queue
	public synchronized boolean addSongQueue(Song song) {
		if (playQueue.size() <= MAX_SONGS_QUEUE) {
			if (song != null) {
				playQueue.add(song);
			}
			return true;
		}
		return false;
	}

	// Clear the queue
	public synchronized void clear() {
		playQueue.clear();
		index = INITIAL_INDEX;
	}

	// Add several songs and clear the queue, the second parameter is the index
	// of the song that should be played first.
	public synchronized boolean addAllSongs(Collection<Song> songs, int startingPosition) {
		if (!songs.isEmpty()) {
			if (songs.size() <= MAX_SONGS_QUEUE) {
				clear();
				playQueue.addAll(songs);
				if (startingPosition < playQueue.size() && startingPosition >= 0)
					// Minus 1 because it'll increment 1 before playing when
					// calling getNext()
					index = startingPosition - 1;
				return true;
			}
		}
		return false;
	}

	// Add several songs to the end of the queue.
	public synchronized boolean addAllSongsQueue(Collection<Song> songs) {
		if (!songs.isEmpty()) {
			if (playQueue.size() + songs.size() <= MAX_SONGS_QUEUE) {
				playQueue.addAll(songs);
				return true;
			}
		}
		return false;
	}

	// Get the next song in the play queue, if the current is the last go back
	// to the first.
	public Song getNext() {
		if (!playQueue.isEmpty()) {
			synchronized (this) {
				incrementIndex();
			}
			return playQueue.get(index);
		}
		return null;
	}

	// Get the previous song in the play queue, if current is the first it
	// returns the it again.
	public Song getPrevious() {
		if (!playQueue.isEmpty()) {
			synchronized (this) {
				decrementIndex();
			}
			return playQueue.get(index);
		}
		return null;
	}

	// Get current song in the play queue
	public Song getCurrent() {
		if (!playQueue.isEmpty()) {
			if (index >= 0) {
				return playQueue.get(index);
			} else {
				return playQueue.get(0);
			}
		}
		return null;
	}

	// Increments index by 1 that represent the position of the current song
	private void incrementIndex() {
		if (index < playQueue.size() - 1)
			index++;
		else
			index = 0;
	}

	// Decrement index by 1 that represent the position of the current song
	private void decrementIndex() {
		if (index > 0)
			index--;
	}

	// Return true if the Music Player is playing any song
	public synchronized boolean isPlaying() {
		return isPlaying;
	}

	// Set the isPlaying flag
	public synchronized void setPlaying(boolean isPlaying) {
		this.isPlaying = isPlaying;
	}

	public Collection<Song> getCollection() {
		return playQueue;
	}

	// Move current song to the one at index=index
	public synchronized boolean moveToSongAt(int index) {
		// Index can be -1, it means that there is not current song and the next
		// one will be the fist of the list
		if (index >= -1 && index < playQueue.size()) {
			this.index = index;
			return true;
		}
		return false;
	}

	// Return a song given its ID, null if there isn't any song with this ID in
	// the playQueue.
	public Song getSongById(int songId) {
		for (int i = 0; i < playQueue.size(); i++) {
			Song song = playQueue.get(i);
			if (song.getId() == songId) {
				return song;
			}
		}
		return null;
	}

}
