package com.example.josip.smartapartmentnew;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;


import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.gms.tasks.Tasks;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
//import com.google.firebase.database.FirebaseError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.READ_CONTACTS;
import static android.R.id.message;
import static android.provider.AlarmClock.EXTRA_MESSAGE;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mFirebaseAuth;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private CheckBox mAutoLoginView;
    private View mProgressView;
    private View mLoginFormView;

    private String mUserUid;
    private DatabaseReference mDatabase;
    private List<String> mApartmentIDs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SharedPreferences prefs = this.getSharedPreferences("PREF_PERSONAL_DATA", Context.MODE_PRIVATE);

        mApartmentIDs = new ArrayList<>();

        //initializing firebase auth object
        mFirebaseAuth = FirebaseAuth.getInstance();

        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email);
        mEmailView.setText(prefs.getString("email", null));

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        mPasswordView.setText(prefs.getString("password", null));

        mAutoLoginView = (CheckBox) findViewById(R.id.auto_sign_in_check_box);
        mAutoLoginView.setChecked(prefs.getBoolean("autoLogin", false));

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });


        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        if (mAutoLoginView.isChecked() == true)
            mEmailSignInButton.callOnClick();
    }



    private void attemptLogin() {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString().trim();
        String password = mPasswordView.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            mFirebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                //showProgress(false);
                                saveLoginData();
                                mDatabase = FirebaseDatabase.getInstance().getReference();
                                mUserUid = task.getResult().getUser().getUid();
                                Toast.makeText(LoginActivity.this, "Successfully sign in", Toast.LENGTH_SHORT).show();
                            } else {
                                showProgress(false);
                                Toast.makeText(LoginActivity.this, "Registration Error", Toast.LENGTH_LONG).show();
                                mPasswordView.setError(getString(R.string.error_incorrect_password));
                                mPasswordView.requestFocus();
                            }
                        }
                    })
                    .continueWithTask(new GetApartmentIDs())
                    .continueWithTask(new GetApartmentDetails())
                    .addOnCompleteListener(this, new OnCompleteListener<String>() {
                                @Override
                                public void onComplete(@NonNull Task<String> task) {
                                    showProgress(false);
                                    if (task.getResult() == "OK")
                                        finish();
                                }
                            });
        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }


    private void saveLoginData() {
        SharedPreferences prefs = this.getSharedPreferences("PREF_PERSONAL_DATA", Context.MODE_PRIVATE);

        prefs.edit().putString("email", mEmailView.getText().toString().trim()).commit();
        prefs.edit().putString("password", mPasswordView.getText().toString().trim()).commit();
        prefs.edit().putBoolean("autoLogin", mAutoLoginView.isChecked()).commit();

    }

    private void saveApartmentData(String key, String value) {
        SharedPreferences prefs = this.getSharedPreferences("PREF_PERSONAL_DATA", Context.MODE_PRIVATE);

        prefs.edit().putString(key, value).commit();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    private class GetApartmentIDs implements Continuation<AuthResult, Task<String>> {
        @Override
        public Task<String> then(Task<AuthResult> task) {
            final TaskCompletionSource<String> tcs = new TaskCompletionSource();

            if (task.isSuccessful()) {
                Log.d("DEBUG", "HASH: " + mUserUid.hashCode());

                mDatabase.child("users").child(mUserUid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                saveApartmentData("numberOfApartments", Long.toString(dataSnapshot.getChildrenCount()));
                                for (DataSnapshot sn : dataSnapshot.getChildren()) {
                                    mApartmentIDs.add(sn.getValue().toString());
                                    saveApartmentData(sn.getKey(), sn.getValue().toString());
                                }
                                tcs.setResult("OK");
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                tcs.setResult("KO");
                            }
                        });
            } else {
                tcs.setResult("KO");
            }
            return tcs.getTask();
        }
    }

    private class GetApartmentDetails implements Continuation<String, Task<String>> {
        @Override
        public Task<String> then(Task<String> task) {
            final TaskCompletionSource<String> tcs = new TaskCompletionSource();

            if (task.getResult() == "OK") {
                for (String str : mApartmentIDs) {
                    mDatabase.child(str).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    String name = dataSnapshot.child("name").getValue().toString();

                                    saveApartmentData(dataSnapshot.getKey().concat("_name"), name);

                                    // podaci o klima uredajima
                                    saveApartmentData(dataSnapshot.getKey().concat("_climate_count"), Long.toString(dataSnapshot.child("Climates").getChildrenCount()));
                                    int i = 1;
                                    for (DataSnapshot ds : dataSnapshot.child("Climates").getChildren()) {
                                        saveApartmentData(dataSnapshot.getKey().concat("_climate_name").concat(Integer.toString(i)), ds.getKey());
                                        saveApartmentData(dataSnapshot.getKey().concat("_climate_id").concat(Integer.toString(i)), ds.getValue().toString());
                                        i++;
                                    }
                                    // podaci o vratima
                                    saveApartmentData(dataSnapshot.getKey().concat("_door_count"), Long.toString(dataSnapshot.child("Doors").getChildrenCount()));
                                    i = 1;
                                    for (DataSnapshot ds : dataSnapshot.child("Doors").getChildren()) {
                                        saveApartmentData(dataSnapshot.getKey().concat("_door_name").concat(Integer.toString(i)), ds.getKey());
                                        saveApartmentData(dataSnapshot.getKey().concat("_door_id").concat(Integer.toString(i)), ds.getValue().toString());
                                        i++;
                                    }
                                    // podaci o kljucevima
                                    saveApartmentData(dataSnapshot.getKey().concat("_key_count"), Long.toString(dataSnapshot.child("Keys").getChildrenCount()));
                                    i = 1;
                                    for (DataSnapshot ds : dataSnapshot.child("Keys").getChildren()) {
                                        saveApartmentData(dataSnapshot.getKey().concat("_key_name").concat(Integer.toString(i)), ds.getValue().toString());
                                        saveApartmentData(dataSnapshot.getKey().concat("_key_id").concat(Integer.toString(i)), ds.getKey());
                                        i++;
                                    }

                                    if (dataSnapshot.getKey().equalsIgnoreCase(mApartmentIDs.get(mApartmentIDs.size() - 1))) {
                                        //showProgress(false);
                                        Toast.makeText(LoginActivity.this, "Retreiving data finished", Toast.LENGTH_SHORT).show();
                                        //finish();
                                        tcs.setResult("OK");
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    tcs.setResult("KO");
                                }
                            });
                }
            } else {
                tcs.setResult("KO");
            }
            return tcs.getTask();
        }
    }
}