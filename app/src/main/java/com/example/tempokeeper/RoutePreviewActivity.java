package com.example.tempokeeper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
        GoogleMap.OnPolygonClickListener {

    private GoogleMap mMap;
    private Button btnBack;
    private Button btnRun;
    private LatLng originPoint;
    private ArrayList<Polyline> routesArray;
    private ArrayList<PolylineOptions> nextActvityArray;

    private static final int REQUEST_LOCATION_PERMISSION = 9003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_route_preview);

        Bundle bundle = getIntent().getExtras();
        String url = bundle.getString("url");
        double latitude = bundle.getDouble("originLat");
        double longitude = bundle.getDouble("originLong");
        originPoint = new LatLng(latitude, longitude);

        btnBack = (Button) findViewById(R.id.btnBack);
        btnRun = (Button) findViewById(R.id.btnSelectMusic);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoutePreviewActivity.this, RouteFormActivity.class);
                startActivity(intent);
            }
        });

        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //idk what to do here lmao
                //user should get a route to run and run it
                //camera follows user location on the route
                //and updates route

                Intent intent = new Intent(RoutePreviewActivity.this, PlaylistActivity.class);
                for (int i = 0; i < routesArray.size(); i++) {
                    if (routesArray.get(i).getColor() == Color.BLUE) {
                        intent.putExtra("chosenRoute", nextActvityArray.get(i));
                        startActivity(intent);
                        break;
                    }
                }
            }
        });

        // Start downloading json data from Google Directions API
        DownloadTask downloadTask = new DownloadTask();
        Log.d("RPA", "downloadTasl.execute");
        downloadTask.execute(url);
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

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

            ParserTask parserTask = new ParserTask();

            Log.d("RPA", "parserTask.execute");
            parserTask.execute(result);

        }
    }


    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

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

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            routesArray = new ArrayList<Polyline>();
            nextActvityArray = new ArrayList<PolylineOptions>();
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);
                Log.d("Parser result.get", result.toString());

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.GRAY);
                lineOptions.geodesic(true);
                lineOptions.clickable(true);
                nextActvityArray.add(lineOptions);
                Polyline polyline = mMap.addPolyline((lineOptions));
//                polyline.setTag("");
                routesArray.add(polyline);
            }
            routesArray.get(0).setZIndex(1);
            routesArray.get(0).setColor(Color.BLUE);
        }
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
        for (int i = 0; i < routesArray.size(); i++) {
            routesArray.get(i).setZIndex(0);
            routesArray.get(i).setColor(Color.GRAY);
            Log.d("onPolylineCLick", "index: " + routesArray.get(i) + ", Index: " + routesArray.get(i).getZIndex());
        }
        polyline.setZIndex(1);
        polyline.setColor(Color.BLUE);
        Log.d("onPolylineCLick", "polyline Index: " + polyline.getZIndex());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        //enables device location to be shown on the map
//        enableMyLocation();
//         Add a marker at Stuvi
        // hard coded
//        LatLng stuvi= new LatLng(42.3521572, -71.1158678);
        mMap.addMarker(new MarkerOptions()
                .position(originPoint)
                .title("Marker at User Location"));
        //hard coded a polyline and set camera there
        Polyline polyline1 = googleMap.addPolyline(new PolylineOptions()
                .clickable(true)
                .add(
                        new LatLng(-35.016, 143.321),
                        new LatLng(-34.747, 145.592),
                        new LatLng(-34.364, 147.891),
                        new LatLng(-33.501, 150.217),
                        new LatLng(-32.306, 149.248),
                        new LatLng(-32.491, 147.309)));
        polyline1.setTag("a");

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(originPoint, 7));

        // Set listeners for click events.
        googleMap.setOnPolylineClickListener(this);
        googleMap.setOnPolygonClickListener(this);

        enableMyLocation();
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
}