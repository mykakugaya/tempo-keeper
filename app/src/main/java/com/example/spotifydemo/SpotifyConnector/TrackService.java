package com.example.spotifydemo.SpotifyConnector;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.example.spotifydemo.Model.EndPoints;
import com.example.spotifydemo.Model.Song;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TrackService {
    private ArrayList<Song> songs = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private String requestURL;      // URL used in API calls
    private String playlistId;      // playlist id if getting tracks of a certain playlist

    public TrackService(SharedPreferences sharedPref) {
        sharedPreferences = sharedPref; // get shared preferences
    }

    // get a user's saved tracks
    public void getUserTracks() {
        requestURL = EndPoints.USER_TRACKS.toString();
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new okhttp3.Request.Builder().url(requestURL)
                .method("GET",null)
                        .addHeader("Authorization", "Bearer " + sharedPreferences.getString("token", ""))
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

    // get the tracks of a specific playlist
    public void getPlaylistTracks() {
        // format the request URL to include the specific playlist ID
        requestURL = String.format(EndPoints.PLAYLIST_TRACKS.toString(), playlistId);
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new okhttp3.Request.Builder().url(EndPoints.USER_TRACKS.toString())
                .method("GET",null)
                .addHeader("Authorization", "Bearer " + sharedPreferences.getString("token", ""))
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
