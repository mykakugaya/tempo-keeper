package com.example.tempokeeper.SpotifyConnector;

import android.content.SharedPreferences;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.tempokeeper.Model.EndPoints;
import com.example.tempokeeper.Model.User;
import com.example.tempokeeper.VolleyCallBack;
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
            /* this conversion automatically saves userId, country, etc.
            in the appropriate fields of the User class */
            user = gson.fromJson(response.toString(), User.class);
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
