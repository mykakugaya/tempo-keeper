package com.example.tempokeeper;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tempokeeper.Model.FirebaseUser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class RegistrationActivity extends AppCompatActivity {

    private EditText regusername;
    private EditText regpassword;
    private EditText regName;
    private Button btnRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_registration);

        regusername = (EditText) findViewById(R.id.regusername);
        regpassword = (EditText) findViewById(R.id.regpassword);
        regName = (EditText) findViewById(R.id.name);
        btnRegister = (Button) findViewById(R.id.btnregister);
        mAuth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(v -> {
            String name = regName.getText().toString();
            String username = regusername.getText().toString();
            String password = regpassword.getText().toString();
            registerUser(name, username, password);
        });
    }

    private void registerUser(String name, String username, String password) {

        //the if statements are edge cases
        if (name.isEmpty()) {
            regName.setError("Name is required");
            regName.requestFocus();
            return;
        }
        if (username.isEmpty()) {
            regusername.setError("Username is required");
            regusername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            regpassword.setError("password is required");
            regpassword.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            regusername.setError("Please provide valid email");
            regusername.requestFocus();
            return;
        }
        if (password.length() < 6) {
            regpassword.setError("Minimum password length should be 6 characters");
            regpassword.requestFocus();
            return;
        }

        mAuth.createUserWithEmailAndPassword(username, password)
                //Creates a new user account associated with the specified email address and password
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = new FirebaseUser(name, username);
                            //create a new class user when it's successfully registered and authenticated
                            FirebaseDatabase.getInstance().getReference("Users")
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        //display success toast and go back to MainActivity.
                                        Toast.makeText(RegistrationActivity.this, username+" registered successfully!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                                    } else {
                                        Toast.makeText(RegistrationActivity.this, "Failed to register", Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });
                        } else {
                            Toast.makeText(RegistrationActivity.this, "Failed to register", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}