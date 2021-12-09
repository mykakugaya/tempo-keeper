package com.example.tempokeeper;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RoutePreviewActivity extends AppCompatActivity implements OnMapReadyCallback,  GoogleMap.OnPolylineClickListener, GoogleMap.OnInfoWindowClickListener{

    // variable that are passed with intents for the statistic activity
    private String dest;
    private double targetDist;

    // Google map for the preview
    private GoogleMap mMap;

    // Navigation buttons for navigating between activities
    private Button btnBack;
    private Button btnMusic;

    //
    private ArrayList<String> eOArray = new ArrayList<String>();

    // Array of Polylines to contain all the possible routes on
    private ArrayList<Polyline> routesArray;

    // Create Array fo Polyline Options to prepare for later Activity
    private ArrayList<PolylineOptions> nextActivityArray;

    // List of lists containing elevations of each possible route coinciding with the routeArray
    private ArrayList<List<Double>>  ElevationArray = new ArrayList<List<Double>>();

    //
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    Marker userLocationMarker;

    //ensures the user's starting Marker doesn't move
    boolean start = false;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 9002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_preview);

        btnBack = (Button) findViewById(R.id.btnBack);
        btnMusic = (Button) findViewById(R.id.btnSelectMusic);
        btnMusic.setEnabled(false);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // set up a location request to receive the location at each second in time
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // get the desired destination from the previous route form activity
        // could also be from clicking the back button in PlaylistActivity
        Bundle bundle = getIntent().getExtras();
        dest = bundle.getString("destination");
        targetDist = bundle.getDouble("targetDist");

        // button to return to the route preview form
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoutePreviewActivity.this, RouteFormActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // initialize the google map
        mMap = googleMap;
        // enable and initialize a window on click listener for the google map
        mMap.setOnInfoWindowClickListener(RoutePreviewActivity.this);

        btnMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // prepare intent for next activity once a route is selected
                Intent intent = new Intent(RoutePreviewActivity.this, PlaylistActivity.class);
                intent.putExtra("destination", dest); // add dest just in case we want to come back to this page
                intent.putExtra("targetDist",targetDist); // dist that is determined during the routes form
                for (int i = 0; i < routesArray.size(); i++){
                    if (routesArray.get(i).getColor() == Color.BLUE){
                        intent.putExtra("chosenRoute", nextActivityArray.get(i)); // pass the chosen route onto next activity
                        startActivity(intent);
                        break;
                    }
                }
            }
        });

        // Set listeners for click events.
        googleMap.setOnPolylineClickListener(this);
        enableMyLocation();
    }

    // location callback to initially run the following code
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Log.d(TAG, "onLocationResult: " + locationResult.getLastLocation());

            //Get the the LatLng value for the current location in order to place marker at current location
            LatLng latLng = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
            //once the map is inflated...
            if (mMap != null) {
                //if there is no origin point marker, create one
                if (userLocationMarker == null) {
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.anchor((float) 0.5, (float) 0.5);
                    markerOptions.title("This is you");
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    userLocationMarker = mMap.addMarker(markerOptions);
                }
                //if the origin point marker exists, get the destination and make the Google
                //Directions API call
                if (!start){
                    Bundle bundle = getIntent().getExtras();
                    String dest = bundle.getString("destination");
                    String url = getDirectionsUrl(userLocationMarker.getPosition(), dest);

                    //get a list of addresses that could match the user's destination input
                    List<Address> addressList = null;
                    MarkerOptions destMarker = new MarkerOptions();
                    Address myAddress;
                    LatLng destlatLng;

                    Geocoder geocoder = new Geocoder(RoutePreviewActivity.this);
                    try {
                        //add possible adresses to a list
                        addressList = geocoder.getFromLocationName(dest, 5);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (addressList != null) {
                        //place a destination marker at the most likely destination
                        for (int i = 0; i < addressList.size(); i++) {
                            myAddress = addressList.get(i);
                            destlatLng = new LatLng(myAddress.getLatitude(), myAddress.getLongitude());
                            destMarker.position(destlatLng);
                            destMarker.title("This is your destination");
                            mMap.addMarker(destMarker);
                        }
                    }

                    //Set the camera to include the origin marker and destination marker
                    //the camera should zoom in so the route is clearly visible
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(userLocationMarker.getPosition());
                    builder.include(destMarker.getPosition());
                    LatLngBounds bounds = builder.build();

                    int width = getResources().getDisplayMetrics().widthPixels;
                    int height = getResources().getDisplayMetrics().heightPixels;
                    int padding = (int) (width * 0.10); // offset from edges of the map 10% of screen

                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);

                    mMap.animateCamera(cu);

                    // Direc Download Task is initialized and executed
                    //Downloads the url created and prepares to get routes
                    DirecDownloadTask direcDownloadTask = new DirecDownloadTask();
                    Log.d("RPA", "direcDownloadTask.execute");
                    direcDownloadTask.execute(url);
                    start = true;
                }
            }
        }
    };

    // format the Directions http url to then recieve the google direction API output
    private String getDirectionsUrl(LatLng origin, String dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        dest.replaceAll("//s", "");
        String str_dest = "destination=" + dest;

        // Sensor enabled
        String mode = "&mode=walking";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + mode;

        // Building the url to the web service
        //alternatives=true asks google directions to return at most 3 possible routes
        String url = "https://maps.googleapis.com/maps/api/directions/json?" + parameters + "&key=AIzaSyDOMa68BrLRKcMLnCVODFd3cwqOxfnB2qw&alternatives=true";

        Log.d("URL", url);
        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Directions API Download Task
    // Used to make an AsyncTask that will get the URL of the Google Directions API Request
    private class DirecDownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {
            //can add getting user location here
            String data = "";

            try {
                // Pass the http URL and receive the data to be parsed
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // create new Directions API Parser Task
            DirecParserTask direcParserTask = new DirecParserTask();

            Log.d("RPA", "direcParserTask.execute");
            // Pass the Parser task the returned URL and finally retrieve usable data from parsing the JSON
            direcParserTask.execute(result);
        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class DirecParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                // create new JSON Object to parse
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // receive the actual parsed and usable data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @SuppressLint("ResourceAsColor")
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            // all points from the route
            ArrayList points = null;
            //adjust certain parameters to be able to graph the polyline routes
            PolylineOptions lineOptions = null;
            // Array of all possible given routes
            routesArray = new ArrayList<Polyline>();
            // Array to prepare for future activities
            nextActivityArray = new ArrayList<PolylineOptions>();
            // Create new Polyline Options to hold elevation data
            PolylineOptions eO = new PolylineOptions();

            if (result.size() == 0){
                // No route are possible from current location to specified destination
                // return to routes form activity and try again with new destination
                Snackbar snackbar = Snackbar.make(btnBack, "Route not possible", Snackbar.LENGTH_SHORT);
                snackbar.getView().setBackgroundColor(R.color.colorPrimaryDark);
                snackbar.show();
            } else {
                // begin to loop through the list of the resulting output of the parse JSON Data
                // this loop should loop through each of the different possible routes
                for (int i = 0; i < result.size(); i++) {
                    points = new ArrayList();
                    lineOptions = new PolylineOptions();

                    List<HashMap<String, String>> path = result.get(i);
                    Log.d("Parser result.get", result.toString());

                    // Loop through the output noting each point from the route in LatLng values
                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);

                        Double lat = Double.parseDouble(point.get("lat"));
                        Double lng = Double.parseDouble(point.get("lng"));

                        LatLng position = new LatLng(lat, lng);

                        // Keep the LatLng values in an ArrayList for easy access and usability in
                        // later functions
                        points.add(position);
                    }

                    // add all points to the route and format it to draw it on the google map
                    lineOptions.addAll(points);
                    lineOptions.width(12);
                    lineOptions.color(Color.GRAY);
                    lineOptions.geodesic(true);
                    lineOptions.clickable(true);
                    nextActivityArray.add(lineOptions);
                    Polyline polyline = mMap.addPolyline((lineOptions));
                    // add route to the routes array
                    routesArray.add(polyline);

                    // check the length of the line and see many instances are taken from the polyline
                    // if the size is greater than 100 we may not be able to retrieve data as the url
                    // will be too long
                    int check = lineOptions.getPoints().size();
                    if (check > 100) {
                        int y = check / 40;
                        for (int w = 0; w < points.size(); w += y) {
                            eO.add((LatLng) points.get(w));
                        }
                    } else {
                        // if the size is less than 100 we are able to use all points and retrieve more
                        // precise data
                        eO.addAll(points);
                    }

                    // prepare a string (strE) for use in the elevation URL request
                    String strE;
                    strE = "" + eO.getPoints();
                    Log.d("good", strE);
                    strE = strE.replaceAll("lat/lng: ", "");
                    strE = strE.replaceAll("[()]", "");
                    strE = strE.replaceAll(", ", "|");
                    strE = strE.replaceAll("[\\[\\]]", "");
                    // add the string to the list that will hold the elevation url parameters
                    eOArray.add(strE);
                }

                // enable next button
                btnMusic.setEnabled(true);

                // finally get the actual elevation values
                // this is done by once again downloading the JSON data using an http request then
                // parsing through the output to get the elevation values into a list of doubles that
                // allows us to work with the values and compare them at different location along the route
                getElevationVals();
            }
        }
    }

    /**
     * Elevation Strategy:
     * 1. get an array of route arrays of latlngs from the JSONParser
     * 2. traverse each route, find the elevation of each latlng, and add the elevation to an array
     * 3. get the difference in elevation for the whole route and compare it with the other routes
     * 4. highlight the one with the greatest or smallest difference depending on user preference
     */

    public void getElevationVals() {
        // repeat for each of the routes that was parsed from the google Directions API
        for (int i = 0; i < routesArray.size(); i++) {
            String elevURL = "https://maps.googleapis.com/maps/api/elevation/json?samples=40&key=AIzaSyDOMa68BrLRKcMLnCVODFd3cwqOxfnB2qw&path=" + eOArray.get(i);
            ElevDownloadTask elevDownloadTask = new ElevDownloadTask();
            // Start downloading json data from Google Directions API
            elevDownloadTask.execute(elevURL);
        }
    }

    private class ElevDownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                // recieve the data output in jSON format from the JSON URL request
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // create and initialize a parser task object
            ElevParserTask elevParserTask = new ElevParserTask();

            // run the parser task
            elevParserTask.execute(result);
        }
    }

    private class ElevParserTask extends AsyncTask<String, Integer, List<HashMap<String, String>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<HashMap<String, String>> elevations = null;

            try {
                // recieve the JSON data and initialize the parser for receiving the elevations
                jObject = new JSONObject(jsonData[0]);
                ElevJSONParser parser = new ElevJSONParser();

                // return the elevation values from the parsing
                elevations = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return elevations;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {
            // initialize array list of the elevation along a single route
            ArrayList<Double> elevations2 = new ArrayList();

            for (int i = 0; i < result.size(); i++) {

                HashMap<String, String> point = result.get(i);

                // get the elevation from the hashmap return value of the Elevation JSON Parser
                double elev = Double.parseDouble(point.get("elev"));
                elevations2.add(elev);
            }

            // add the array list to the ElevationsArray that will hold the list of elevations
            // for each of the routes possible from current location to specified destination
            ElevationArray.add(elevations2);

            // initialize values and sum all values to determine which route had the
            // highest elevation along the route
            Double sum0 = 0.0;
            Double sum1 = 0.0;
            Double sum2 = 0.0;
            for(int i =0;i<ElevationArray.size();i++){
                for (int j=0;j<ElevationArray.get(i).size();j++) {
                    if (i == 0){
                        sum0+=ElevationArray.get(i).get(j);
                    } else if (i == 1){
                        sum1+=ElevationArray.get(i).get(j);
                    } else if (i == 2){
                        sum2+=ElevationArray.get(i).get(j);
                    }
                }
            }

            // Assign each of the routes a tag showing the results of our elevation parsing
            // the elevations will be compared to the other options for routes compared to
            // an arbitrary threshold
            if (sum0 != 0 && sum1 == 0 && sum2 == 0) {
                routesArray.get(0).setTag("");
            } else if (sum0 != 0 && sum1 != 0 && sum2 == 0) {
                if (sum0 >= sum1){
                    routesArray.get(0).setTag("This route is the most flat");
                    routesArray.get(1).setTag("This route is the most hilly");
                } else {
                    routesArray.get(1).setTag("This route is the most flat");
                    routesArray.get(0).setTag("This route is the most hilly");
                }
            } else if (sum0 != 0 && sum1 != 0 && sum2 != 0) {
                if (sum0 >= sum1 & sum0 >= sum2) {
                    routesArray.get(0).setTag("This route is the most hilly");
                    if (sum1 >= sum2) {
                        routesArray.get(1).setTag("This route is less hilly");
                        routesArray.get(2).setTag("This route is the most flat");
                    } else {
                        routesArray.get(2).setTag("This route is less hilly");
                        routesArray.get(1).setTag("This route is the most flat");
                    }
                } else if (sum1 >= sum2 & sum1 >= sum0) {
                    routesArray.get(1).setTag("This route is the most hilly");
                    if (sum2 >= sum0) {
                        routesArray.get(2).setTag("This route is less hilly");
                        routesArray.get(0).setTag("This route is the most flat");
                    } else {
                        routesArray.get(0).setTag("This route is less hilly");
                        routesArray.get(2).setTag("This route is the most flat");
                    }
                } else if (sum2 >= sum0 & sum2 >= sum1) {
                    routesArray.get(2).setTag("This route is the most hilly");
                    if (sum0 >= sum1) {
                        routesArray.get(0).setTag("This route is less hilly");
                        routesArray.get(1).setTag("This route is the most flat");
                    } else {
                        routesArray.get(1).setTag("This route is less hilly");
                        routesArray.get(0).setTag("This route is the most flat");
                    }
                }
            }

            // recieve from the bundle which type of route the user desired from Hilly, Flat, or No preference
            Bundle bundle = getIntent().getExtras();
            String elev = bundle.getString("elevation");

            // Set the default route to fit the user's desired parameters
            if (elev.equals("Hilly")){
                for (int i =0;i<routesArray.size();i++){
                    if(routesArray.get(i).getTag() == "This route is the most hilly"){
                        routesArray.get(i).setColor(Color.BLUE);
                        routesArray.get(i).setZIndex(1);
                    } else {
                        routesArray.get(i).setColor(Color.GRAY);
                        routesArray.get(i).setZIndex(0);
                    }
                }
            } else {
                for (int i =0;i<routesArray.size();i++){
                    if(routesArray.get(i).getTag() == "This route is the most flat"){
                        routesArray.get(i).setColor(Color.BLUE);
                        routesArray.get(i).setZIndex(1);
                    } else {
                        routesArray.get(i).setColor(Color.GRAY);
                        routesArray.get(i).setZIndex(0);
                    }
                }
            }
            //if there is only 1 route
            if (routesArray.get(0).getTag() == ""){
                routesArray.get(0).setColor(Color.BLUE);
            }
        }
    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {
        // initialize all routes to gray (deselected mode)
        for (int i = 0; i < routesArray.size(); i++){
            routesArray.get(i).setZIndex(0);
            routesArray.get(i).setColor(Color.GRAY);
            Log.d("onPolylineCLick",  "index: " + routesArray.get(i) + ", Index: " + routesArray.get(i).getZIndex());
        }
        // set the polyline that was selected to rise to the top and change color to
        // blue indication this is the selected route that the user desired to take
        polyline.setZIndex(1);
        polyline.setColor(Color.BLUE);
        Log.d("onPolylineCLick",  "polyline Index: " + polyline.getZIndex());

        if (!polyline.getTag().equals("")) {
            Toast.makeText(getBaseContext(), ""+polyline.getTag(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {}

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            //request permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }

    // MENU
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // Handle menu clicks by the user
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // go to create route form
        if (id == R.id.menuRoute) {
            Intent routeIntent = new Intent(RoutePreviewActivity.this, RouteFormActivity.class);
            startActivity(routeIntent);
            return true;
        }

        // go to user profile
        if (id == R.id.menuProfile) {
            Intent profileIntent = new Intent(RoutePreviewActivity.this, ProfileActivity.class);
            startActivity(profileIntent);
            return true;
        }

        // sign out of app
        if (id == R.id.menuSignOut) {
            FirebaseAuth.getInstance().signOut();

            // user is now signed out, show toast
            Toast.makeText(getBaseContext(), "Signed out.", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(RoutePreviewActivity.this, LoginActivity.class));
            finish();

            return true;
        }

        // default
        return super.onOptionsItemSelected(item);

    }
}