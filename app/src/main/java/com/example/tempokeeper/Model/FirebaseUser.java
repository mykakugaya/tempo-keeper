package com.example.tempokeeper.Model;

public class FirebaseUser {
    // a User class to keep the necessary profile data of a user, this would be expanded much more in the future.
    public String name, username;

    public FirebaseUser(){

    }

    public FirebaseUser(String name, String username){
        this.name = name;
        this.username = username;

    }
}
