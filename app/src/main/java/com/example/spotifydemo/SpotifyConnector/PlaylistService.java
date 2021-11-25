package com.example.spotifydemo.SpotifyConnector;

import android.content.SharedPreferences;
import android.util.Log;

import com.example.spotifydemo.Model.EndPoints;
import com.example.spotifydemo.Model.Playlist;
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

public class PlaylistService {


    private SharedPreferences sharedPreferences;
    private String requestURL;  // URL for API calls

    // recycler view variables for custom list adapter
    private ArrayList<Playlist> playlists = new ArrayList<>();  // array of playlists to display

    // OkHTTPClient used to send http requests and read responses
    private Call mCall;
    private OkHttpClient mOkHttpClient = new OkHttpClient();

    public PlaylistService(SharedPreferences sharedPref) {
        sharedPreferences = sharedPref; // contains userId, playlistId
    }

    public ArrayList<Playlist> getPlaylists() {return playlists;}

    // get a user's own and followed playlists
    public void getUserPlaylists() {
        // assign requestURL for GET request
        requestURL = EndPoints.USER_PLAYLISTS.toString();

        // use OkHttpClient to build a new request with token in authorization header
        // token is stored in SharedPreferences
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
                // convert json array object into array of playlist objects
                addToPlaylists(jsonObject);
            } catch (JSONException e) {
                // failed to parse
                Log.d("Error", String.valueOf(e));
                e.printStackTrace();
            }
        }
    });
}

    // cancel the current HTTP call request
    private void cancelCall() {
        if (mCall != null) {
            mCall.cancel();
        }
    }

    // format string json object into playlist class object
    public void addToPlaylists(JSONObject response) {
        // using Gson again to convert JSON object to Playlist object
        Gson gson = new Gson();
        JSONArray jsonArray = response.optJSONArray("items");

        // loop through the array of playlists, create playlist object instance for each item
        for (int n = 0; n < jsonArray.length(); n++) {
            try {
                // converting to playlist object - this will set the name of the playlist
                JSONObject jsonObject = jsonArray.getJSONObject(n);
                Playlist playlist = gson.fromJson(jsonObject.toString(), Playlist.class);

                // set playlist number of tracks by getting the length of the "tracks" field
                playlist.setNumTracks(jsonObject.optJSONObject("tracks").getInt("total"));

                // set playlist image url if available
                try {
                    playlist.setImageURL(jsonObject.optJSONArray("images").optJSONObject(0).getString("url"));
                } catch (NullPointerException e) {
                    playlist.setImageURL(null);
                }

                // add each playlist object to array of playlists
                playlists.add(playlist);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }


}
