package com.example.simplemessenger.ui.main;

import static com.example.simplemessenger.utils.AuthUtils.checkAuthState;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.simplemessenger.R;
import com.example.simplemessenger.SimpleMessengerApp;
import com.example.simplemessenger.databinding.ActivityMessageListBinding;
import com.example.simplemessenger.ui.auth.AuthActivity;
import com.example.simplemessenger.ui.contacts.ContactsFragment;
import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.ui.contacts.ManageContactsActivity;
import com.example.simplemessenger.ui.messaging.ComposeMessageActivity;
import com.example.simplemessenger.ui.messaging.MessageListFragment;
import com.example.simplemessenger.ui.profile.ProfileActivity;
import com.example.simplemessenger.ui.settings.SettingsActivity;
import com.example.simplemessenger.ui.config.FirebaseConfigActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.Nullable;

public class MainActivity extends AppCompatActivity {

    private void showError(String message) {
        runOnUiThread(() -> {
            try {
                Log.e("MainActivity", "Showing error: " + message);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e("MainActivity", "Error showing error toast", e);
            }
        });
    }
    
    private void showErrorAndFinish(String message) {
        runOnUiThread(() -> {
            try {
                Log.e("MainActivity", "Showing fatal error: " + message);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                new android.os.Handler().postDelayed(() -> {
                    try {
                        finishAffinity();
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error finishing activity", e);
                    }
                }, 2000);
            } catch (Exception e) {
                Log.e("MainActivity", "Error showing error message", e);
                try {
                    finishAffinity();
                } catch (Exception ex) {
                    // Last resort
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }
        });
    }

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize and set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Set the Toolbar title and disable the home button (since this is the main activity)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
        }
        
        // Initialize floating action button
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            // Handle FAB click (e.g., compose new message)
            // startActivity(new Intent(this, ComposeMessageActivity.class));
        });
        
        // Load the initial fragment if this is the first time
        if (savedInstanceState == null) {
            navigateToMessageList();
        }
        
        // Initialize UI components
        try {
            // Initialize Firebase Auth
            mAuth = FirebaseAuth.getInstance();
            Log.d("MainActivity", "Firebase Auth initialized");
            
            // Initialize SharedPreferences
            sharedPreferences = getSharedPreferences(
                    SimpleMessengerApp.SHARED_PREFS_NAME,
                    MODE_PRIVATE
            );
            Log.d("MainActivity", "SharedPreferences initialized");
            
            // Initialize the UI
            initializeUI();
            
        } catch (Exception e) {
            Log.e("MainActivity", "Error during initialization", e);
            showErrorAndFinish("Error initializing app. Please restart.");
        }
    }
    
    private void initializeUI() {
        try {
            Log.d("MainActivity", "Initializing UI");

            // Set up ViewPager
            try {
                // Create the adapter that will return the fragment
                mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this);
                Log.d("MainActivity", "SectionsPagerAdapter created");

                // Set up the ViewPager with the sections adapter
                mViewPager = findViewById(R.id.container);
                if (mViewPager == null) {
                    throw new IllegalStateException("ViewPager not found in layout");
                }
                mViewPager.setAdapter(mSectionsPagerAdapter);
                Log.d("MainActivity", "ViewPager adapter set");
            } catch (Exception e) {
                Log.e("MainActivity", "Error setting up ViewPager", e);
                showErrorAndFinish("Error initializing app interface. Please restart the app.");
                return;
            }

            // Set up FAB
            FloatingActionButton fab = findViewById(R.id.fab);
            if (fab != null) {
                fab.setOnClickListener(view -> {
                    try {
                        startActivity(new Intent(MainActivity.this, ComposeMessageActivity.class));
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error starting ComposeMessageActivity", e);
                        showError("Cannot open message composer. Please try again.");
                    }
                });
                Log.d("MainActivity", "FAB set up");
            } else {
                Log.w("MainActivity", "FAB not found in layout");
            }

            // Check auth state
            checkAuthState(this, AuthActivity.class, MainActivity.class);
            Log.d("MainActivity", "onCreate completed successfully");
        } catch (Exception e) {
            Log.e("MainActivity", "Error in initializeUI", e);
            showErrorAndFinish("Error initializing UI. Please restart the app.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume");
        
        // Check authentication state when activity resumes
        if (mAuth.getCurrentUser() == null) {
            Log.d("MainActivity", "User not authenticated, finishing activity");
            finish();
            return;
        }
        
        // Initialize and load contacts
        try {
            Log.d("MainActivity", "Initializing contacts");
            ContactsManager contactsManager = ContactsManager.getInstance();
            contactsManager.initializeContacts();
            Log.d("MainActivity", "ContactsManager initialized");
        } catch (Exception e) {
            Log.e("MainActivity", "Error initializing contacts", e);
            showError("Error loading contacts. Please try again.");
        }
        
        checkAuthState(this, AuthActivity.class, MainActivity.class);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity", "onPause");
        
        // Clean up contacts manager when app goes to background
        try {
            ContactsManager contactsManager = ContactsManager.getInstance();
            contactsManager.cleanup();
            Log.d("MainActivity", "ContactsManager cleaned up in onPause");
        } catch (Exception e) {
            Log.e("MainActivity", "Error cleaning up ContactsManager in onPause", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "onDestroy");
        
        // Clean up any references to prevent memory leaks
        mViewPager = null;
        mSectionsPagerAdapter = null;
        
        // Clean up contacts manager
        try {
            ContactsManager contactsManager = ContactsManager.getInstance();
            contactsManager.cleanup();
            Log.d("MainActivity", "ContactsManager cleaned up in onDestroy");
        } catch (Exception e) {
            Log.e("MainActivity", "Error cleaning up ContactsManager in onDestroy", e);
        }
        
        mAuth = null;
        sharedPreferences = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Handle home/up button press
        if (id == android.R.id.home) {
            // If there are fragments in the back stack, pop the top one
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
                return true;
            }
            // If no fragments in back stack, let the system handle it
            onBackPressed();
            return true;
        } else if (id == R.id.action_settings) {
            navigateToSettings();
            return true;
        } else if (id == R.id.action_profile) {
            navigateToProfile();
            return true;
        } else if (id == R.id.action_contacts) {
            navigateToContacts();
            return true;
        } else if (id == R.id.action_logout) {
            confirmLogout();
            return true;
        } else if (id == R.id.action_firebase_settings) {
            navigateToFirebaseSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    // Navigation methods
    public void navigateToMessageList() {
        MessageListFragment fragment = MessageListFragment.newInstance(0); // 0 is the default section number
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
        
        // Update toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_messages);
        }
    }
    
    private void navigateToSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }
    
    private void navigateToProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
    }
    
    private void navigateToContacts() {
        ContactsFragment contactsFragment = ContactsFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, contactsFragment)
                .addToBackStack("contacts")
                .commit();
                
        // Update toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.menu_contacts);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    private void navigateToFirebaseSettings() {
        startActivity(new Intent(this, FirebaseConfigActivity.class));
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        // Handle back button in the toolbar
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return true;
        }
        return super.onSupportNavigateUp();
    }
    
    @Override
    public void onBackPressed() {
        // If there are fragments in the back stack, pop them
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
    
    // Helper method to update the toolbar title
    public void updateToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_logout)
                .setMessage(R.string.confirm_logout)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> logout())
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void logout() {
        try {
            // Sign out from Firebase
            mAuth.signOut();
            
            // Clear SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            
            // Go back to AuthActivity
            Intent intent = new Intent(this, AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                          Intent.FLAG_ACTIVITY_NEW_TASK | 
                          Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e("MainActivity", "Error during logout", e);
            showError("Error during logout. Please try again.");
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final Context context;

        public SectionsPagerAdapter(FragmentManager fm, Context context) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.context = context;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return MessageListFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return 1; // Only one section for messages
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return context.getString(R.string.title_messages);
        }
    }
}
