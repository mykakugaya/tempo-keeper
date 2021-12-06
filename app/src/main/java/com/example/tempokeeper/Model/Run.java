package com.example.tempokeeper.Model;

import android.widget.ImageView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Run {
    private int index;
    private String date;    // e.g. "12/01/21"
    private String duration;  // e.g. "2:31:03"
    private String distance;
    private String avgSpeed;
    private String maxSpeed;
    private ArrayList<LatLng> route;
    private String imgRoute;

    public Run(int index) {
        this.index = index;
    }

    public void setDate(String date) {this.date = date;}

    public String getDate() {return this.date;}

    public void setRoute(ArrayList<LatLng> route) {this.route = route;}

    public ArrayList<LatLng> getRoute() {return this.route;}

    public void setDuration(String duration) {this.duration = duration;}

    public String getDuration() {return this.duration;}

    public void setDistance(String distance) {this.distance = distance;}

    public String getDistance() {return this.distance;}

    public void setAvgSpeed(String speed) {this.avgSpeed = speed;}

    public String getAvgSpeed() {return this.avgSpeed;}

    public void setMaxSpeed(String speed) {this.maxSpeed = speed;}

    public String getMaxSpeed() {return this.maxSpeed;}

    public String getImage() {
        return imgRoute;
    }

    public void setImage(String image) {
        this.imgRoute = image;
    }
}