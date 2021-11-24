package com.example.spotifydemo.Model;

public enum EndPoints {

    RECENTLY_PLAYED("https://api.spotify.com/v1/me/player/recently-played"),
    USER_TRACKS("https://api.spotify.com/v1/me/tracks"),
    PLAYLIST_TRACKS("https://api.spotify.com/v1/playlists/%s/tracks"),
    USER_PLAYLISTS("https://api.spotify.com/v1/me/playlists"),
    ENDPOINT_AUDIO("https://api.spotify.com/v1/audio-features/"),
    USER("https://api.spotify.com/v1/me"),
    ;

    private final String endpoint;

    EndPoints(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public String toString(){
        return endpoint;
    }
}
