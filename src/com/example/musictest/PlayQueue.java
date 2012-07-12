package com.example.musictest;


import java.util.Collection;
import java.util.Vector;

public class PlayQueue {

	private Vector<Song> playQueue = null;
	private static PlayQueue singleton;
	public static final int INITIAL_INDEX = -1;
	private int index = INITIAL_INDEX;

	private PlayQueue() {
		playQueue = new Vector<Song>();
	}

	public static PlayQueue getInstance() {
		if (singleton == null) {
			singleton = new PlayQueue();
		}
		return singleton;
	}

	public void addSong(Song song) {
		if (song != null) {
			clear();
			playQueue.add(song);
		}
	}

	public void addSongQueue(Song song) {
		if (song != null) {
			;
			playQueue.add(song);
		}
	}

	public void clear() {
		playQueue.clear();
		index = INITIAL_INDEX;
	}

	public void addAllSongs(Collection<Song> songs, int startingPosition) {
		if (!songs.isEmpty()) {
			clear();
			playQueue.addAll(songs);
			if (startingPosition < playQueue.size() && startingPosition >= 0)
				//Minus 1 because it'll increment 1 before playing when calling getNext()
				index = startingPosition-1; 
		}
	}

	public void addAllSongsQueue(Collection<Song> songs) {
		if (!songs.isEmpty()) {
			playQueue.addAll(songs);
		}
	}

	public Song getNext() {
		if (!playQueue.isEmpty()) {
			synchronized (this) {
				incrementIndex();
			}
			return playQueue.get(index);
		}
		return null;
	}

	public Song getPrevious() {
		if (!playQueue.isEmpty()) {
			synchronized (this) {
				decrementIndex();
			}
			return playQueue.get(index);
		}
		return null;
	}

	public Song getCurrent() {
		if (index >= 0) {
			if (!playQueue.isEmpty()) {
				return playQueue.get(index);
			}
		}
		return null;
	}

	private void incrementIndex() {
		if (index < playQueue.size() - 1)
			index++;
		else
			index = 0;
	}

	private void decrementIndex() {
		if (index > 0)
			index--;
	}

}
