package com.example.tempokeeper.Model;

public class Playlist {
    private String id;
    private String imageURL;
    private String name;
    private int numTracks;

    public Playlist(String id, String name){
        this.id = id;
        this.name = name;
    }

    // methods to get and set the playlist information
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public int getNumTracks() { return this.numTracks; }

    public void setNumTracks(int numTracks) { this.numTracks = numTracks; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
