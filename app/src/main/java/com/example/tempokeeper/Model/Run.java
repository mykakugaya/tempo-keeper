package com.example.tempokeeper.Model;

import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class Run {
    private int index;
    private long startTime; // in ms
    private long finishTime;   // in ms
    private long duration;  // in ms
    private double distance;
    private double avgSpeed;
    private double maxSpeed;
    private ArrayList<PolylineOptions> route;

    public Run() {

    }

    public void setIndex(int index) {this.index = index;}

    public int getIndex() {return this.index;}

    public void setDuration(long duration) {this.duration = duration;}

    public long getDuration() {return this.duration;}

    public void setDistance(int distance) {this.distance = distance;}

    public double getDistance() {return this.distance;}

    public void setStartTime(long startTime) {this.startTime = startTime;}

    public long getStartTime() {return this.startTime;}

    public void setFinishTime(long finishTime) {this.finishTime = finishTime;}

    public long getFinishTime() {return this.finishTime;}

    public void setAvgSpeed(double speed) {this.avgSpeed = speed;}

    public double getAvgSpeed() {return this.avgSpeed;}

    public void setMaxSpeed(double speed) {this.maxSpeed = speed;}

    public double getMaxSpeed() {return this.maxSpeed;}
}