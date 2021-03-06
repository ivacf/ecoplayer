/*
 * Author:	Ivan Carballo Fernandez (icf1e11@soton.ac.uk) 
 * Project:	EcoPlayer - Battery-friendly music player for Android (MSc project at University of Southampton)
 * Date:	13-07-2012
 * License: Copyright (C) 2012 Ivan Carballo. 
 */
package com.ecoplayer.beta;

import android.os.Parcel;
import android.os.Parcelable;

//Representation of a music album
public class Album implements Parcelable {

	private String title = null;
	private String artist = null;
	//Key of the album that uniquely identify it
	private String key = null;
	private int numSongs = 0;

	public Album(String key, String title, String artist, int numSongs) {
		this.title = title;
		this.artist = artist;
		this.key = key;
		this.numSongs = numSongs;
	}

	private Album(Parcel in) {
		this.title = in.readString();
		this.key = in.readString();
		this.artist = in.readString();
		this.numSongs = in.readInt();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getNumSongs() {
		return numSongs;
	}

	public void setNumSongs(int numSongs) {
		this.numSongs = numSongs;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(title);
		out.writeString(key);
		out.writeString(artist);
		out.writeInt(numSongs);
	}

	public static final Parcelable.Creator<Album> CREATOR = new Parcelable.Creator<Album>() {
		public Album createFromParcel(Parcel in) {
			return new Album(in);
		}

		public Album[] newArray(int size) {
			return new Album[size];
		}
	};

}
