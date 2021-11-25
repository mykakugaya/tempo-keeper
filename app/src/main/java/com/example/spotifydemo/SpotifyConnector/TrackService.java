package com.example.spotifydemo.SpotifyConnector;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.example.spotifydemo.Model.EndPoints;
import com.example.spotifydemo.Model.Playlist;
import com.example.spotifydemo.Model.Track;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TrackService {
    private ArrayList<Track> tracks = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private String requestURL;      // URL used in API calls
    private String playlistId;      // playlist id for getting tracks of a certain playlist

    // OkHTTPClient used to send http requests and read responses
    private Call mCall;
    private OkHttpClient mOkHttpClient = new OkHttpClient();

    // TrackService constructor
    public TrackService(SharedPreferences sharedPref) {
        sharedPreferences = sharedPref; // get shared preferences for token
    }

    // constructor for getting the tracks of a specific playlist
    public TrackService(SharedPreferences sharedPref, String id) {
        sharedPreferences = sharedPref; // get shared preferences for token
        playlistId = id;    // the id of the specific playlist
    }

    public ArrayList<Track> getTracks() {return tracks;}

    // get the tracks of a specific playlist
    public ArrayList<Track> getPlaylistTracks() {
        // assign requestURL for GET request
        // format string so that playlist id replaces the "%s" in endpoint
        requestURL = String.format(EndPoints.PLAYLIST_TRACKS.toString(), playlistId);

        // use OkHttpClient to build a new request with token in authorization header
        Request request = new Request.Builder().url(requestURL)
                .method("GET",null)
                .addHeader("Authorization", "Bearer " + sharedPreferences.getString("TOKEN", ""))
                .build();

        // clear previous requests
        cancelCall();

        // create new call for the request
        mCall = mOkHttpClient.newCall(request);

        // queue up the request
        mCall.enqueue(new Callback() {
            // request failed, print error
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            // got a successful response from the call
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    // get the response body as new json array object
                    final JSONObject jsonObject = new JSONObject(response.body().string());
                    System.out.println(jsonObject);
                    // convert json array object into array of Track objects
                    addToTracks(jsonObject);
                } catch (JSONException e) {
                    // failed to parse
                    Log.d("Error", String.valueOf(e));
                    e.printStackTrace();
                }
            }
        });
        return tracks;
    }

    // cancel the current HTTP call request
    private void cancelCall() {
        if (mCall != null) {
            mCall.cancel();
        }
    }

    // format string json object into playlist class object
    public void addToTracks(JSONObject response) {
        // using Gson again to convert JSON object to Track object
        Gson gson = new Gson();
        JSONArray jsonArray = response.optJSONArray("items");

        // loop through the array of tracks, create Track object instance for each item
        for (int n = 0; n < jsonArray.length(); n++) {
            try {
                // converting to Track object
                JSONObject jsonObject = jsonArray.getJSONObject(n);
                Track track = gson.fromJson(jsonObject.toString(), Track.class);
                // set track image url if available
                try {
                    track.setImageURL(jsonObject.optJSONArray("images").optJSONObject(0).getString("url"));
                } catch (NullPointerException e) {
                    track.setImageURL(null);
                }
                // add each track object to array of tracks
                tracks.add(track);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }


}
