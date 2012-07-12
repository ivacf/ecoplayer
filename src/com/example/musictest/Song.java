package com.example.musictest;

import com.ecoplayer.beta.R;

public class Song {

	private String title = null;
	private int id = 0;
	private Album album = null;
	private boolean isPlaying = false;

	public Song(int id, String title, Album album) {
		this.title = title;
		this.id = id;
		this.album=album;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	public void setPlaying(boolean isPlaying) {
		this.isPlaying = isPlaying;
	}

	public Album getAlbum() {
		return album;
	}

	public void setAlbum(Album album) {
		this.album = album;
	}

}
