package com.example.spotifydemo.Model;

public enum EndPoints {
    USER("https://api.spotify.com/v1/me"),
    USER_PLAYLISTS("https://api.spotify.com/v1/me/playlists"),
    PLAYLIST_TRACKS("https://api.spotify.com/v1/playlists/%s/tracks"),
    TRACK_AUDIO("https://api.spotify.com/v1/audio-features"),
    ADD_TO_QUEUE("https://api.spotify.com/v1/me/player/queue"),
    START_PLAYBACK("https://api.spotify.com/v1/me/player/play"),
    NEXT_TRACK("https://api.spotify.com/v1/me/player/next"),
//    RECENTLY_PLAYED("https://api.spotify.com/v1/me/player/recently-played"),
//    USER_TRACKS("https://api.spotify.com/v1/me/tracks"),
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
