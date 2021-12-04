package com.example.tempokeeper.Model;

import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class Run {
    private int index;
    private String date;    // e.g. "12/01/21"
    private String duration;  // e.g. "2:31:03"
    private double distance;
    private double avgSpeed;
    private double maxSpeed;

    public Run(int index) {
        this.index = index;
    }

    public void setDate(String date) {this.date = date;}

    public String getDate() {return this.date;}

    public void setDuration(String duration) {this.duration = duration;}

    public String getDuration() {return this.duration;}

    public void setDistance(int distance) {this.distance = distance;}

    public double getDistance() {return this.distance;}

    public void setAvgSpeed(double speed) {this.avgSpeed = speed;}

    public double getAvgSpeed() {return this.avgSpeed;}

    public void setMaxSpeed(double speed) {this.maxSpeed = speed;}

    public double getMaxSpeed() {return this.maxSpeed;}
}