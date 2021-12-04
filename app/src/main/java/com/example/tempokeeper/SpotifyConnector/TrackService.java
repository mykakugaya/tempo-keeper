package com.example.tempokeeper.SpotifyConnector;

import android.content.SharedPreferences;
import android.util.Log;

import com.example.tempokeeper.Model.EndPoints;
import com.example.tempokeeper.Model.Track;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TrackService {
    // tracks array to store the request response items
    private ArrayList<Track> tracks = new ArrayList<>();
    private String playlistId;

    // tracksAdded is set to true when tracks array if filled after a get request
    public boolean tracksAdded = false;

    // sharedPreferences with API call token
    private final SharedPreferences sharedPreferences;

    // OkHTTPClient used to send http requests and read responses
    private Call mCall;
    private OkHttpClient mOkHttpClient = new OkHttpClient();

    // TrackService constructor
    public TrackService(SharedPreferences sharedPref) {
        sharedPreferences = sharedPref; // get shared preferences for token
    }

    public ArrayList<Track> getTracks() {return tracks;}

    public void setTracks(ArrayList<Track> newTracks) {tracks = newTracks; }

    // get the tracks of a specific playlist
    // pass in the specific playlist id
    public void getPlaylistTracks(String id) {
        playlistId = id;
        tracksAdded = false;
        // create the request URL for GET request
        // format string so that playlist id replaces the "%s" in endpoint
        String requestURL = String.format(EndPoints.PLAYLIST_TRACKS.toString(), playlistId);

        // use OkHttpClient to build a GET request with request URL and token in authorization header
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

                    // convert json array object into array of Track objects
                    addToTracks(jsonObject);

                    // once all tracks from playlist have been added
                    // addToTracks sets tracksAdded to true
                    // Now we can call setTrackTempos to perform the audio features get request
                    if(tracksAdded) {
                        setTrackTempos();
                    }

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
    public void addToTracks(JSONObject response) {
        // using Gson again to convert JSON object to Track object
        Gson gson = new Gson();
        JSONArray jsonArray = response.optJSONArray("items");

        // loop through the array of tracks, create Track object instance for each item
        for (int n = 0; n < jsonArray.length(); n++) {
            try {
                // Convert each JSON object into a Track object
                // This sets the track's id and name, duration, etc.
                JSONObject jsonObject = jsonArray.getJSONObject(n).getJSONObject("track");
                Track track = gson.fromJson(jsonObject.toString(), Track.class);

                // set the track's artist to the main artist in the artists array
                track.setArtist(jsonObject.optJSONArray("artists").optJSONObject(0).getString("name"));

                // set the track's album name if available
                // this may just be an empty string if the track does not belong to an album
                track.setAlbumName(jsonObject.optJSONObject("album").getString("name"));

                // set track album image url if available
                try {
                    track.setImageURL(jsonObject.optJSONObject("album").optJSONArray("images").optJSONObject(0).getString("url"));
                } catch (NullPointerException e) {
                    track.setImageURL(null);
                }

                // set the playlist id of the track
                track.setPlaylistId(playlistId);

                // add each track object to array of tracks
                tracks.add(track);

                // signal that all tracks from playlist have been added to the tracks array
                // this will signal the setTrackTempos() call to get tempos of the tracks
                if (n == jsonArray.length()-1) {
                    tracksAdded = true;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // method to get the tempo of each track
    public void setTrackTempos() {
        // First build the query string of track ids by looping over tracks array
        // Example of query "ids": "7ouMYWpwJ422jRcDASZB7P,4VqPOruhp5EdPBeR92t6lQ,2takcwOaAZWiXQijPHIx7B"
        StringBuilder sb = new StringBuilder();
        // We loop over the global tracks array, appending each track id to the stringBuilder
        for (int i=0; i<tracks.size(); i++) {
            if (i == tracks.size()-1) {
                sb.append(tracks.get(i).getId());
            } else {
                sb.append(tracks.get(i).getId()).append(",");
            }
        }
        // get the final string of concatenated track ids
        String trackIds = sb.toString();

        // build the Http URL for the audio_features endpoint
        // by adding the trackIds string as a query parameter
        HttpUrl.Builder httpBuilder = HttpUrl.parse(EndPoints.TRACK_AUDIO.toString())
                .newBuilder().addQueryParameter("ids",trackIds);

        // use OkHttpClient to build a new request with token in authorization header
        Request request = new Request.Builder().url(httpBuilder.build())
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
                    // get the response body as new json object
                    final JSONObject jsonObject = new JSONObject(response.body().string());

                    // Response was a JSON object with a JSON array called "audio_features"
                    // The audio_features JSON array holds JSON objects with audio features
                    // for each requested track
                    JSONArray jsonArray = jsonObject.optJSONArray("audio_features");

                    // Loop over the array of audio features for the requested tracks
                    for (int n = 0; n < jsonArray.length(); n++) {
                        try {
                            // We want to get the "tempo" field of each object
                            double trackTempo = jsonArray.optJSONObject(n).getDouble("tempo");
                            // set the tempo of each track in the tracks array
                            Track track = tracks.get(n);
                            track.setTempo(trackTempo);
                            tracks.set(n,track);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                } catch (JSONException e) {
                    // failed to parse
                    Log.d("Error", String.valueOf(e));
                    e.printStackTrace();
                }
            }
        });
    }

}

