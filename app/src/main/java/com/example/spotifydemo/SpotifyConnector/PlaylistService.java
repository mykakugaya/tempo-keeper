package com.example.spotifydemo.SpotifyConnector;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.example.spotifydemo.Model.EndPoints;
import com.example.spotifydemo.Model.Playlist;
import com.example.spotifydemo.Model.Song;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlaylistService {
    // store playlists in an array
    private ArrayList<Playlist> playlists = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private String playlistId;  // specific id for each playlist
    private String requestURL;  // URL for API calls

    public PlaylistService(SharedPreferences sharedPref) {
        sharedPreferences = sharedPref; // contains userId, playlistId
    }

    // get a user's own and followed playlists
    public void getUserPlaylists() {
        requestURL = EndPoints.USER_PLAYLISTS.toString();
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new okhttp3.Request.Builder().url(requestURL)
                .method("GET",null)
                .addHeader("Authorization", "Bearer " + sharedPreferences
                        .getString("token", ""))
                .build();
        AsyncTask.execute(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);

                System.out.println(response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }



}
