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

public class RoutePreviewActivity extends AppCompatActivity implements OnMapReadyCallback,  GoogleMap.OnPolylineClickListener,
        GoogleMap.OnPolygonClickListener, GoogleMap.OnInfoWindowClickListener{

    private String dest;
    private GoogleMap mMap;
    private Button btnBack;
    private Button btnMusic;
    private String linePoints;
    private ArrayList<String> eOArray = new ArrayList<String>();
    private ArrayList<Polyline> routesArray;
    private ArrayList<PolylineOptions> nextActivityArray;
    private ArrayList<List<Double>>  ElevationArray = new ArrayList<List<Double>>();


    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    Marker userLocationMarker;
    Circle userLocationAccuracyCircle;

    boolean start = false;

    private static final int REQUEST_LOCATION_PERMISSION = 9003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_preview);

        btnBack = (Button) findViewById(R.id.btnBack);
        btnMusic = (Button) findViewById(R.id.btnSelectMusic);
        btnMusic.setEnabled(false);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(500);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // get the desired destination from the previous route form activity
        // could also be from clicking the back button in PlaylistActivity
        Bundle bundle = getIntent().getExtras();
        dest = bundle.getString("destination");

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoutePreviewActivity.this, RouteFormActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {

    }

    private class DirecDownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {
            //can add getting user location here
            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            DirecParserTask direcParserTask = new DirecParserTask();

            Log.d("RPA", "direcParserTask.execute");
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
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @SuppressLint("ResourceAsColor")
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            routesArray = new ArrayList<Polyline>();
            nextActivityArray = new ArrayList<PolylineOptions>();
            MarkerOptions markerOptions = new MarkerOptions();
            PolylineOptions eO = new PolylineOptions();

            if (result.size() == 0){
                Snackbar snackbar = Snackbar.make(btnBack, "Route not possible", Snackbar.LENGTH_SHORT);
                snackbar.getView().setBackgroundColor(R.color.colorPrimaryDark);
                snackbar.show();
//                Toast.makeText(RoutePreviewActivity.this, "Route not possible", Toast.LENGTH_SHORT).show();
            } else {
                for (int i = 0; i < result.size(); i++) {
                    points = new ArrayList();
                    lineOptions = new PolylineOptions();

                    List<HashMap<String, String>> path = result.get(i);
                    Log.d("Parser result.get", result.toString());

                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);

                        Double lat = Double.parseDouble(point.get("lat"));
                        Double lng = Double.parseDouble(point.get("lng"));

                        LatLng position = new LatLng(lat, lng);

                        points.add(position);
                    }

                    lineOptions.addAll(points);
                    lineOptions.width(12);
                    lineOptions.color(Color.GRAY);
                    lineOptions.geodesic(true);
                    lineOptions.clickable(true);
                    nextActivityArray.add(lineOptions);
                    Polyline polyline = mMap.addPolyline((lineOptions));
                    //                polyline.setTag("");
                    routesArray.add(polyline);

                    //            Log.d("bye", "before loop: "+eO.getPoints().size());
                    int check = lineOptions.getPoints().size();
                    if (check > 100) {
                        int y = check / 40;
                        //                Log.d("bye", "true");
                        for (int w = 0; w < points.size(); w += y) {
                            eO.add((LatLng) points.get(w));
                        }
                    } else {
                        eO.addAll(points);
                    }

                    Log.d("Hello", "" + lineOptions.getPoints().size());
                    Log.d("bye", "" + eO.getPoints().size());
                    String strE;
                    strE = "" + eO.getPoints();
                    Log.d("good", strE);
                    strE = strE.replaceAll("lat/lng: ", "");
                    strE = strE.replaceAll("[()]", "");
                    strE = strE.replaceAll(", ", "|");
                    strE = strE.replaceAll("[\\[\\]]", "");
                    linePoints = "" + lineOptions.getPoints();
                    eOArray.add(strE);
                }
                //            routesArray.get(0).setZIndex(1);
                //            routesArray.get(0).setColor(Color.BLUE);
                btnMusic.setEnabled(true);
                getElevationVals();
            }
//            Toast.makeText(getApplicationContext(), "Click the paths to see which is the most hilly", Toast.LENGTH_SHORT).show();
        }
    }

    private class ElevDownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ElevParserTask elevParserTask = new ElevParserTask();

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
                jObject = new JSONObject(jsonData[0]);
                ElevJSONParser parser = new ElevJSONParser();

                elevations = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return elevations;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {
            ArrayList<Double> elevations2 = new ArrayList();

            for (int i = 0; i < result.size(); i++) {

                HashMap<String, String> point = result.get(i);

                double elev = Double.parseDouble(point.get("elev"));
                elevations2.add(elev);
            }

            ElevationArray.add(elevations2);

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
            Bundle bundle = getIntent().getExtras();
            String elev = bundle.getString("elevation");
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
//            Toast.makeText(getApplicationContext(), "Ready for Elevations", Toast.LENGTH_SHORT).show();
        }
    }

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

    @Override
    public void onPolygonClick(@NonNull Polygon polygon) {
    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {
        for (int i = 0; i < routesArray.size(); i++){
            routesArray.get(i).setZIndex(0);
            routesArray.get(i).setColor(Color.GRAY);
            Log.d("onPolylineCLick",  "index: " + routesArray.get(i) + ", Index: " + routesArray.get(i).getZIndex());
        }
        polyline.setZIndex(1);
        polyline.setColor(Color.BLUE);
        Log.d("onPolylineCLick",  "polyline Index: " + polyline.getZIndex());

        Toast.makeText(getBaseContext(), ""+polyline.getTag(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(RoutePreviewActivity.this);

        btnMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //user should get a route to run and run it
                //camera follows user location on the route
                //and updates route

                Intent intent = new Intent(RoutePreviewActivity.this, PlaylistActivity.class);
                intent.putExtra("destination", dest); // add dest just in case we want to come back to this page
                for (int i = 0; i < routesArray.size(); i++){
                    if (routesArray.get(i).getColor() == Color.BLUE){
                        intent.putExtra("chosenRoute", nextActivityArray.get(i));
                        startActivity(intent);
                        break;
                    }
                }
            }
        });

        // Set listeners for click events.
        googleMap.setOnPolylineClickListener(this);
        googleMap.setOnPolygonClickListener(this);

        enableMyLocation();
    }

    public void getElevationVals() {
        for (int i = 0; i < routesArray.size(); i++) {
            String elevURL = "https://maps.googleapis.com/maps/api/elevation/json?samples=40&key=AIzaSyDOMa68BrLRKcMLnCVODFd3cwqOxfnB2qw&path=" + eOArray.get(i);
            ElevDownloadTask elevDownloadTask = new ElevDownloadTask();
            // Start downloading json data from Google Directions API
            elevDownloadTask.execute(elevURL);
        }
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Log.d(TAG, "onLocationResult: " + locationResult.getLastLocation());

            LatLng latLng = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
            if (mMap != null) {
                if (userLocationMarker == null) {
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.anchor((float) 0.5, (float) 0.5);
                    markerOptions.title("This is you");
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    userLocationMarker = mMap.addMarker(markerOptions);
                }
                if (!start){
                    Bundle bundle = getIntent().getExtras();
                    String dest = bundle.getString("destination");
                    String url = getDirectionsUrl(userLocationMarker.getPosition(), dest);

                    List<Address> addressList = null;
                    MarkerOptions destMarker = new MarkerOptions();
                    Address myAddress;
                    LatLng destlatLng;

                    Geocoder geocoder = new Geocoder(RoutePreviewActivity.this);
                    try {
                        addressList = geocoder.getFromLocationName(dest, 5);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (addressList != null) {
                        for (int i = 0; i < addressList.size(); i++) {
                            myAddress = addressList.get(i);
                            destlatLng = new LatLng(myAddress.getLatitude(), myAddress.getLongitude());
                            destMarker.position(destlatLng);
                            destMarker.title("This is your destination");
                            mMap.addMarker(destMarker);
                        }
                    }


                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(userLocationMarker.getPosition());
                    builder.include(destMarker.getPosition());
                    LatLngBounds bounds = builder.build();

                    int width = getResources().getDisplayMetrics().widthPixels;
                    int height = getResources().getDisplayMetrics().heightPixels;
                    int padding = (int) (width * 0.10); // offset from edges of the map 10% of screen

                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);

                    mMap.animateCamera(cu);

                    DirecDownloadTask direcDownloadTask = new DirecDownloadTask();
                    Log.d("RPA", "direcDownloadTask.execute");
                    direcDownloadTask.execute(url);
                    start = true;
                }
            }
        }
    };

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            // you need to request permissions...
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
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
                    REQUEST_LOCATION_PERMISSION);
        }
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

            // user is now signed out
            Toast.makeText(getBaseContext(), "Signed out.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(RoutePreviewActivity.this, LoginActivity.class));
            finish();

            return true;
        }

        // default
        return super.onOptionsItemSelected(item);

    }
}