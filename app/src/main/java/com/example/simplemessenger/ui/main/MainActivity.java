package com.example.simplemessenger.ui.main;

import static com.example.simplemessenger.utils.AuthUtils.checkAuthState;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import com.example.simplemessenger.ui.auth.AuthActivity;
import com.example.simplemessenger.ui.messaging.ComposeMessageActivity;
import com.example.simplemessenger.ui.profile.ProfileActivity;
import com.example.simplemessenger.ui.settings.SettingsActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.ui.messaging.adapter.MessageAdapter;
import com.example.simplemessenger.ui.messaging.MessageDetailActivity;
import com.example.simplemessenger.utils.AuthUtils;

import java.util.ArrayList;
import java.util.List;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
        Log.d("MainActivity", "onCreate started");
        super.onCreate(savedInstanceState);
        
        // Set content view first
        setContentView(R.layout.activity_main);
        
        // Initialize UI components
        try {
            // Initialize Firebase Auth
            mAuth = FirebaseAuth.getInstance();
            Log.d("MainActivity", "Firebase Auth initialized");
            
            // Set up the Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(true);
                getSupportActionBar().setTitle(R.string.app_name);
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
            
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
            
            // Set up the Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayShowTitleEnabled(true);
                }
                Log.d("MainActivity", "Toolbar set up");
            }
            
            // Set up ViewPager and TabLayout
            try {
                // Create the adapter that will return a fragment for each section
                mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
                Log.d("MainActivity", "SectionsPagerAdapter created");

                // Set up the ViewPager with the sections adapter
                mViewPager = findViewById(R.id.container);
                if (mViewPager == null) {
                    throw new IllegalStateException("ViewPager not found in layout");
                }
                mViewPager.setAdapter(mSectionsPagerAdapter);
                Log.d("MainActivity", "ViewPager adapter set");

                TabLayout tabLayout = findViewById(R.id.tabs);
                if (tabLayout == null) {
                    throw new IllegalStateException("TabLayout not found in layout");
                }
                mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
                tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
                Log.d("MainActivity", "TabLayout set up");
            } catch (Exception e) {
                Log.e("MainActivity", "Error setting up ViewPager/TabLayout", e);
                showErrorAndFinish("Error initializing app interface. Please restart the app.");
                return;
            }

            // Set up FAB
            try {
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
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error setting up FAB", e);
                // Continue without FAB
            }

            // Check auth state
            checkAuthState(this, AuthActivity.class, MainActivity.class);
            Log.d("MainActivity", "onCreate completed successfully");
            
        } catch (Exception e) {
            Log.e("MainActivity", "Fatal error in onCreate", e);
            showErrorAndFinish("A critical error occurred. The app will now close.");
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
        checkAuthState(this, AuthActivity.class, MainActivity.class);
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

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            confirmLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";
        private RecyclerView recyclerView;
        private MessageAdapter adapter;
        private List<Message> messages = new ArrayList<>();
        private DatabaseHelper databaseHelper;
        private ValueEventListener messageListener;
        private Query messagesQuery;

        public PlaceholderFragment() {
        }

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            databaseHelper = DatabaseHelper.getInstance();
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                               Bundle savedInstanceState) {
            int section = 1;
            if (getArguments() != null) {
                section = getArguments().getInt(ARG_SECTION_NUMBER);
            }

            // Inflate the appropriate layout based on the section
            View rootView;
            if (section == 1) { // Messages section
                rootView = inflater.inflate(R.layout.fragment_messages, container, false);
                setupMessagesRecyclerView(rootView);
            } else {
                rootView = inflater.inflate(R.layout.fragment_main, container, false);
                TextView textView = rootView.findViewById(R.id.section_label);
                String text = getString(R.string.title_messages);
                switch (section) {
                    case 2:
                        text = getString(R.string.title_reminders);
                        break;
                    case 3:
                        text = getString(R.string.title_profile);
                        break;
                }
                textView.setText(text);
            }
            return rootView;
        }

        private void setupMessagesRecyclerView(View rootView) {
            recyclerView = rootView.findViewById(R.id.recycler_view);
            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
                
                // Initialize adapter with empty list
                adapter = new MessageAdapter(new MessageAdapter.OnMessageActionListener() {
                    @Override
                    public void onMessageSelected(Message message) {
                        // Open message detail
                        Intent intent = new Intent(requireContext(), MessageDetailActivity.class);
                        intent.putExtra("message_id", message.getId());
                        startActivity(intent);
                    }

                    @Override
                    public void onMessageLongClicked(Message message) {
                        // Handle long click if needed
                    }
                });
                recyclerView.setAdapter(adapter);
                
                // Load messages
                loadMessages();
            }
        }

        private void loadMessages() {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                return;
            }

            String currentUserId = currentUser.getUid();
            messagesQuery = databaseHelper.getDatabaseReference()
                    .child("user-messages")
                    .child(currentUserId)
                    .child("received")
                    .orderByChild("timestamp");

            messageListener = messagesQuery.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<Message> messageList = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Message message = snapshot.getValue(Message.class);
                        if (message != null) {
                            messageList.add(message);
                        }
                    }
                    updateMessages(messageList);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("PlaceholderFragment", "Error loading messages", databaseError.toException());
                }
            });
        }

        private void updateMessages(List<Message> newMessages) {
            if (getActivity() == null) return;
            
            getActivity().runOnUiThread(() -> {
                messages.clear();
                if (newMessages != null) {
                    messages.addAll(newMessages);
                }
                if (adapter != null) {
                    adapter.updateMessages(messages);
                }
                
                // Show empty state if no messages
                View emptyView = getView() != null ? getView().findViewById(R.id.empty_view) : null;
                if (emptyView != null) {
                    emptyView.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
                    if (recyclerView != null) {
                        recyclerView.setVisibility(messages.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                }
            });
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            // Remove the listener when the view is destroyed
            if (messageListener != null && messagesQuery != null) {
                messagesQuery.removeEventListener(messageListener);
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.title_messages);
                case 1:
                    return getString(R.string.title_reminders);
                case 2:
                    return getString(R.string.title_profile);
            }
            return null;
        }
    }
}
