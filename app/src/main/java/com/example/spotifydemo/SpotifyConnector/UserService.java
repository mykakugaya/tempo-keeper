package com.example.spotifydemo.SpotifyConnector;

import android.content.SharedPreferences;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.spotifydemo.Model.EndPoints;
import com.example.spotifydemo.Model.User;
import com.example.spotifydemo.VolleyCallBack;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class UserService {

    // User variables
    private SharedPreferences msharedPreferences;
    private RequestQueue reqQueue;
    private static User user;

    // RequestQueue and SharedPreferences from SpotifyOAuth
    public UserService(RequestQueue queue, SharedPreferences sharedPreferences) {
        reqQueue = queue;   // contains user api requests
        msharedPreferences = sharedPreferences; // contains token and userid
    }

    public static User getUser() {
        return user;
    }

    /* Get user profile */
    public void getUserProfile(final VolleyCallBack callBack) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(EndPoints.USER.toString(), null, response -> {
            /* Gson convert Java Objects into their JSON representation or vice versa.
                Use methods fromJson() and toJson() to convert.
            */
            // Here, use Gson to convert JSON response into a User object
            Gson gson = new Gson();
            user = gson.fromJson(response.toString(), User.class);  // saves userId, country, etc.
            // VolleyCallBack checks for successful response
            callBack.onSuccess();
        }, error -> getUserProfile(() -> {

        })) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                // Must get the token saved during login to make all API calls
                // Put in the headers under "Authorization" as "Bearer <token>"
                String token = msharedPreferences.getString("TOKEN", "");
                String auth = "Bearer " + token;
                headers.put("Authorization", auth);
                return headers;
            }
        };
        // Added the request to the RequestQueue
        // It will be executed promptly
        reqQueue.add(jsonObjectRequest);
    }
}
