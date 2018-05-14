/**
 * DogGO is a an app that provides a way to improve the social aspect of dog walking in
 *  and around college campuses. DogGO implements two users: a dog walker (which broadcasts
 *  their current location via Firebase) and a dog petter (which broadcasts their
 *  current location and the location of dog walkers via Firebase)
 *
 * CPSC 312-02, Fall 2017
 * Programming Assignment: Final Project
 * Sources:
 *  Dr. Gina Sprint
 *  Android Docs
 *  Stack Overflow
 *  Floating Text Button: https://github.com/dimorinny/floating-text-button
 *
 * @author Kurt Lamon, Andrew Yang
 * @version v1.0 December 8, 2017
 *
 *  doggo icon logo: logomakr.com/3FNRhG
 *  paw print marker logo: https://logomakr.com/3Golco
 *
 *  dog walker icon: <div>Icons made by <a href="http://www.freepik.com" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a> is licensed by <a href="http://creativecommons.org/licenses/by/3.0/" title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a></div>
 *  dog petter icon: <div>Icons made by <a href="http://www.freepik.com" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a> is licensed by <a href="http://creativecommons.org/licenses/by/3.0/" title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a></div>
 *
 **/

/**
 * Main Activity implements Firebase sign in and allows users to progress to PetActivity or
 *  WalkActivity via "Find a dog" button or "Go for a walk" button
 *
 */
package com.kurtlemon.doggo3;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    // Request code for Firebase sign in.
    private final int SIGN_IN_REQUEST = 1;

    // Firebase user ID. Later used as the key for database DogLocations. Initialized to be empty for
    //  debugging purposes.
    private String userID = "";

    // Buttons to go to each type of map activity (Pet/ Walk).
    Button walkerButton;
    Button petterButton;

    // Firebase authentication fields.
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    /** onCreate() function runs as app is created.
     *
     *  Sets up button functionality and initializes Firebase authentication things
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Button functionality for deciding which map activity to go to.
        walkerButton = (Button) findViewById(R.id.walkerButton);
        petterButton = (Button) findViewById(R.id.petterButton);

        walkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent walkIntent = new Intent(MainActivity.this, WalkActivity.class);
                walkIntent.putExtra("userID", userID);
                startActivity(walkIntent);
            }
        });
        petterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent petIntent = new Intent(MainActivity.this, PetActivity.class);
                startActivity(petIntent);
            }
        });

        // Firebase authentication set up
        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null) {
                    // User is signed in.
                    setUpUserSignedIn(user);
                }else{
                    // New user or user is signed out.
                    // Start an activity for the user to sign in.
                    Intent intent = AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setIsSmartLockEnabled(false)
                            .setAvailableProviders(
                                    Arrays.asList(
                                            new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER)
                                                    .build())).build();
                    startActivityForResult(intent, SIGN_IN_REQUEST);
                }
            }
        };
    }

    /** When the activity resumes, re-add the authStateListener so it's running again.
     *
     */
    @Override
    protected void onResume() {
        super.onResume();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    /** When the activity is paused, remove the authStateListener so it's not running in the
     *      background.
     *
     */
    @Override
    protected void onPause() {
        super.onPause();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }

    /** When the user is signed in, save their user ID for later use in saving unique locations in
     *      the database.
     *
     * @param user
     */
    private void setUpUserSignedIn(FirebaseUser user){
        //get the user's id
        userID = user.getUid();
    }

    /** Menu set up actions.
     *
     *  Assigns functionality to the sign out button.
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.sign_out_menu:
                // sign out
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** Creates the options menu as defines in menu_main.xml
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

}
