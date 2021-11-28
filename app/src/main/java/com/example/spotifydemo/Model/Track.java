package com.example.spotifydemo.Model;

public class Track {

    private String id;
    private String name;
    private String artist;
    private String albumName;
    private String imageURL;
    private int duration_ms;    // duration of track in ms
    private String playlistId;
    private double tempo;

    public Track(String id, String name) {
        this.name = name;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }

    public void setTempo(double tempo) { this.tempo = tempo; }

    public double getTempo() { return this.tempo; }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public int getDuration() {
        return duration_ms;
    }

    public void setDuration(int duration) {
        this.duration_ms = duration;
    }
}

