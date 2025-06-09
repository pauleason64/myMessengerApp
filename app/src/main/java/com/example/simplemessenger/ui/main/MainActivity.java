package com.example.SImpleMessenger.ui.main;

import static com.example.SImpleMessenger.utils.AuthUtils.checkAuthState;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.SImpleMessenger.SimpleMessengerApp;
import com.example.SImpleMessenger.R;
import com.example.SImpleMessenger.data.ContactsManager;
import com.example.SImpleMessenger.ui.auth.AuthActivity;
import com.example.SImpleMessenger.ui.config.FirebaseConfigActivity;
import com.example.SImpleMessenger.ui.contacts.ContactsFragment;
import com.example.SImpleMessenger.ui.messaging.ComposeMessageActivity;
import com.example.SImpleMessenger.ui.messaging.MessageListFragment;
import com.example.SImpleMessenger.ui.settings.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private ViewPagerAdapter viewPagerAdapter;

    // Fragments
    private MessageListFragment inboxFragment;
    private MessageListFragment outboxFragment;
    private MessageListFragment notesFragment;
    private ContactsFragment contactsFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        try {
            // Initialize Firebase Auth
            mAuth = FirebaseAuth.getInstance();
            
            // Check if user is authenticated
            if (mAuth.getCurrentUser() == null) {
                startActivity(new Intent(this, AuthActivity.class));
                finish();
                return;
            }
            
            // Initialize views
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            
            // Initialize fragments
            inboxFragment = MessageListFragment.newInstance(0); // Inbox
            outboxFragment = MessageListFragment.newInstance(1); // Outbox
            notesFragment = MessageListFragment.newInstance(2); // Notes
            contactsFragment = new ContactsFragment();
            
            // Set up ViewPager2 with fragments
            viewPager = findViewById(R.id.view_pager);
            setupViewPager(viewPager);
            
            // Check if we need to navigate to contacts tab
            if (getIntent() != null && getIntent().getBooleanExtra("navigate_to_contacts", false)) {
                viewPager.setCurrentItem(3, false);
                bottomNavigationView.setSelectedItemId(R.id.navigation_contacts);
            }
            
            // Set up bottom navigation
            bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_inbox) {
                    viewPager.setCurrentItem(0, false);
                    return true;
                } else if (itemId == R.id.navigation_outbox) {
                    viewPager.setCurrentItem(1, false);
                    return true;
                } else if (itemId == R.id.navigation_notes) {
                    viewPager.setCurrentItem(2, false);
                    return true;
                } else if (itemId == R.id.navigation_contacts) {
                    viewPager.setCurrentItem(3, false);
                    return true;
                }
                return false;
            });
            
            // Disable ViewPager swiping
            viewPager.setUserInputEnabled(false);
            
            // Sync ViewPager with BottomNavigationView
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    switch (position) {
                        case 0:
                            bottomNavigationView.setSelectedItemId(R.id.navigation_inbox);
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle(R.string.label_inbox);
                            }
                            updateFabVisibility(true);
                            updateFabIcon(0);
                            break;
                        case 1:
                            bottomNavigationView.setSelectedItemId(R.id.navigation_outbox);
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle(R.string.label_outbox);
                            }
                            updateFabVisibility(true);
                            updateFabIcon(1);
                            break;
                        case 2:
                            bottomNavigationView.setSelectedItemId(R.id.navigation_notes);
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle(R.string.label_notes);
                            }
                            updateFabVisibility(true);
                            updateFabIcon(2);
                            break;
                        case 3:
                            bottomNavigationView.setSelectedItemId(R.id.navigation_contacts);
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle(R.string.menu_contacts);
                            }
                            updateFabVisibility(false);
                            break;
                    }
                }
            });
            
            // Set initial title and FAB state
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.label_inbox);
            }
            updateFabIcon(0);
            updateFabVisibility(true);
            
            // Initialize SharedPreferences
            sharedPreferences = getSharedPreferences(
                    SimpleMessengerApp.SHARED_PREFS_NAME,
                    MODE_PRIVATE
            );
            
            // Initialize the UI
            initializeUI();
            
        } catch (Exception e) {
            Log.e("MainActivity", "Error during initialization", e);
            showErrorAndFinish("Error initializing app. Please restart.");
        }
    }
    
    private void setupViewPager(ViewPager2 viewPager) {
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPagerAdapter.addFragment(inboxFragment, getString(R.string.label_inbox));
        viewPagerAdapter.addFragment(outboxFragment, getString(R.string.label_outbox));
        viewPagerAdapter.addFragment(notesFragment, getString(R.string.label_notes));
        viewPagerAdapter.addFragment(contactsFragment, getString(R.string.menu_contacts));
        viewPager.setAdapter(viewPagerAdapter);
    }
    
    private void updateFabVisibility(boolean show) {
        FloatingActionButton fab = findViewById(R.id.fab);
        if (fab != null) {
            if (show) {
                fab.show();
            } else {
                fab.hide();
            }
        }
    }
    
    private void updateFabIcon(int position) {
        FloatingActionButton fab = findViewById(R.id.fab);
        if (fab != null) {
            if (position == 2) { // Notes tab
                fab.setImageResource(R.drawable.ic_note_add);
            } else {
                fab.setImageResource(R.drawable.ic_add);
            }
        }
    }
    
    private void initializeUI() {
        try {
            // Set up FAB for compose message/note
            FloatingActionButton fab = findViewById(R.id.fab);
            if (fab != null) {
                fab.setOnClickListener(view -> {
                    try {
                        // Get the current tab position
                        int currentTab = viewPager.getCurrentItem();
                        Intent composeIntent = new Intent(MainActivity.this, ComposeMessageActivity.class);
                        
                        // Set common extras for all compose actions
                        composeIntent.putExtra(ComposeMessageActivity.EXTRA_COMPOSE_NEW, true);
                        
                        // Set mode-specific extras
                        if (currentTab == 2) { 
                            // Notes tab - compose new note
                            composeIntent.putExtra(ComposeMessageActivity.EXTRA_IS_NOTE, true)
                                        .putExtra(ComposeMessageActivity.EXTRA_NOTE_MODE, true);
                            
                            Log.d(TAG, "Launching ComposeMessageActivity in NOTE mode");
                        } else { 
                            // Inbox or Outbox tab - compose new message
                            composeIntent.putExtra(ComposeMessageActivity.EXTRA_IS_NOTE, false)
                                        .putExtra(ComposeMessageActivity.EXTRA_NOTE_MODE, false);
                            
                            Log.d(TAG, "Launching ComposeMessageActivity in MESSAGE mode");
                        }
                        
                        // Start the activity
                        startActivity(composeIntent);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling FAB click", e);
                        showError("Cannot perform this action. Please try again.");
                    }
                });
            }
            
            // Set initial FAB state based on the current tab
            int currentItem = viewPager.getCurrentItem();
            updateFabVisibility(currentItem != 3); // Hide FAB on Contacts tab
            updateFabIcon(currentItem);

            // Check auth state
            checkAuthState(this, AuthActivity.class, MainActivity.class);
        } catch (Exception e) {
            Log.e("MainActivity", "Error in initializeUI", e);
            showErrorAndFinish("Error initializing UI. Please restart the app.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Check authentication state when activity resumes
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        
        // Check auth state - this will handle redirecting to login if needed
        checkAuthState(this, AuthActivity.class, MainActivity.class);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Clean up contacts manager when app goes to background
        try {
            ContactsManager contactsManager = ContactsManager.getInstance();
            contactsManager.cleanup();
        } catch (Exception e) {
            Log.e("MainActivity", "Error cleaning up ContactsManager in onPause", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up any references to prevent memory leaks
        viewPager = null;
        viewPagerAdapter = null;
        
        // Clean up contacts manager
        try {
            ContactsManager contactsManager = ContactsManager.getInstance();
            contactsManager.cleanup();
        } catch (Exception e) {
            Log.e("MainActivity", "Error cleaning up ContactsManager in onDestroy", e);
        }
        
        mAuth = null;
        sharedPreferences = null;
    }
    
    private void showError(String message) {
        runOnUiThread(() -> {
            try {
                Log.e(TAG, "Showing error: " + message);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing error toast", e);
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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Handle home/up button press
        if (id == android.R.id.home) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
                return true;
            }
            onBackPressed();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_contacts) {
            // Navigate to the contacts tab
            viewPager.setCurrentItem(3, true);
            bottomNavigationView.setSelectedItemId(R.id.navigation_contacts);
            return true;
        } else if (id == R.id.action_logout) {
            confirmLogout();
            return true;
        } else if (id == R.id.action_firebase_settings) {
            startActivity(new Intent(this, FirebaseConfigActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
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
     * ViewPager2 Adapter that manages fragments for bottom navigation
     */
    private static class ViewPagerAdapter extends FragmentStateAdapter {
        private final List<Fragment> fragments = new ArrayList<>();
        private final List<String> fragmentTitles = new ArrayList<>();

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        void addFragment(Fragment fragment, String title) {
            fragments.add(fragment);
            fragmentTitles.add(title);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }
        
        CharSequence getPageTitle(int position) {
            return fragmentTitles.get(position);
        }
    }
}
