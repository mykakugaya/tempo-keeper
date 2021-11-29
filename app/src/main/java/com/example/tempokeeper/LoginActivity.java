package com.example.tempokeeper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.tempokeeper.Model.User;
import com.example.tempokeeper.SpotifyConnector.UserService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

/* Main code sources for Spotify OAuth:
https://towardsdatascience.com/using-the-spotify-api-with-your-android-application-the-essentials-1a3c1bc36b9e
https://developer.spotify.com/documentation/android/quick-start/
https://developer.spotify.com/documentation/android/guides/
https://developer.spotify.com/documentation/web-api/reference/#/
*/

/**
 * Flow: LoginActivity/RegistrationActivity -> RouteFormActivity -> RoutePreviewActivity ->
 * PlaylistActivity -> PedometerActivity+Map
 * */

/* This class uses Firebase authentication to log in, it also
authenticates the app to access the user's Spotify account */
public class LoginActivity extends AppCompatActivity {
    private EditText username;
    private EditText password;
    private Button btnlogin;
    private Button btnregister;
    private FirebaseAuth mAuth;

    // SharedPreferences to save user info
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    // Add requests to Volley.RequestQueue
    private RequestQueue queue;

    // Initialize Spotify variables
    private static final String CLIENT_ID = "f48deef1d46e49a8a445952351b10216";
    // Scopes from: https://developer.spotify.com/documentation/general/guides/authorization/scopes/
    // Permissions required for API calls
    private static final String SCOPES = "app-remote-control,playlist-read-private,playlist-modify-private,playlist-modify-public,user-read-private,streaming,user-top-read,user-modify-playback-state,user-read-currently-playing,user-read-playback-state";

    // Request code will be used to verify if result comes from the login activity. Can be set to any integer.
    private static final int REQUEST_CODE = 1337;

    // Redirect URI in case of failed response
    private static final String REDIRECT_URI = "com.example.tempokeeper://callback/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_login);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        btnlogin = (Button) findViewById(R.id.login);
        btnregister = (Button) findViewById(R.id.register);
        mAuth = FirebaseAuth.getInstance(); //A FirebaseDatabase instance.


        // This is where we will save the user ID and token obtained in the login activity
        sharedPref = getSharedPreferences("SPOTIFY", Context.MODE_PRIVATE);

        /* Volley is an HTTP library that allows automatic scheduling of network requests,
         you can prioritize and cancel requests */
        queue = Volley.newRequestQueue(this);

        // Login button
        // Clicking login will authenticate Spotify and attempt to sign the user in
        // with Firebase auth
        btnlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = username.getText().toString();
                String pwd = password.getText().toString();
                // Authorize this app to access your Spotify app information
                authenticateSpotify();
                signIn(email, pwd);
            }
        });

        // Register if user does not have an account
        btnregister.setOnClickListener(new View.OnClickListener() {
            //when register button is clicked, go to the registration page
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(i);   //would start the registration activity
            }
        });

    }

    private void signIn(String email, String password) {
        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                //Asynchronously signs in using an email and password.
                //Fails with an error if the email address and password do not match.
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //when it's signed in correctly, give a Toast with success message and go to Userinfo activity.
                            Toast.makeText(LoginActivity.this, "Login Success",
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), RouteFormActivity.class));

                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, "Login Failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        // [END sign_in_with_email]
    }

    // Logging in user, get user information
    void loadSpotifyInfo() {
        // Create new UserService with RequestQueue and sharedPreferences
        // Each user has their Spotify userId and token stored in sharedPreferences
        editor = sharedPref.edit();
        UserService userService = new UserService(queue, sharedPref);
        /* getUserProfile creates a User instance with Spotify account
            information such as username, country, etc. */
        userService.getUserProfile(() -> {
            User user = userService.getUser();  // get current user
            editor.putString("userId", user.id);  // save user ID in sharedPreferences
            editor.apply();

            // Alert user that they are logged in as: userid
            Log.d("OAUTH", "GOT USER INFO");
            Toast.makeText(LoginActivity.this, "Logged in as: "+user.id, Toast.LENGTH_SHORT).show();

            // Go to next activity: SearchByBpm
            Intent newIntent = new Intent(LoginActivity.this, PlaylistActivity.class);
            startActivity(newIntent);
        });
    }

    // Spotify requests require authorization - OAuth access token
    /* OAuth 2.0 authorization framework enables a third-party
    application to obtain limited access to an HTTP service */
    private void authenticateSpotify() {
        // Open AuthenticationRequest with Client ID, response type (auth token), and redirect URI
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);

        // Shows a pop up dialogue - user clicks okay to allow access to Spotify account
        builder.setShowDialog(true);

        // Set our requested scopes - request permissions to grant personal information
        builder.setScopes(new String[]{SCOPES});

        // Build and send the request
       /* If Spotify is installed on the device, the auth-lib will connect to the Spotify client
        and fetch the authorization code/access token for the current user. */
        /* If Spotify is not installed on the device, the auth-lib will fallback to the
        WebView based authorization and open the Spotify Accounts login page */
        AuthorizationRequest request = builder.build();
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    /* Handles response to AuthenticationRequest: redirect from Spotify
        user has logged in, token received if successful */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, data);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    editor = sharedPref.edit();
                    // get the access token and save token in SharedPreferences
                    /* We will include this token in all future API calls to Spotify
                    * Put in the headers "authorization" field */
                    String token = response.getAccessToken();
                    this.editor.putString("TOKEN", token);
                    editor.apply();
                    Log.d("OAUTH", "GOT AUTH TOKEN");

                    // create a new User object
                    loadSpotifyInfo();
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Toast.makeText(getApplicationContext(), "Spotify auth flow returned error.", Toast.LENGTH_LONG).show();
                    break;

                // Most likely auth flow was cancelled
                default:
                    Toast.makeText(getApplicationContext(), "Spotify auth flow cancelled.", Toast.LENGTH_LONG).show();
                    break;
            }
        }

    }
}