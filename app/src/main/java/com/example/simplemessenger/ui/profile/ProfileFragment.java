package com.example.simplemessenger.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.UserProfileManager;
import com.example.simplemessenger.data.model.User;
import com.example.simplemessenger.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private UserProfileManager profileManager;
    private String userId;

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        profileManager = UserProfileManager.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set up click listeners
        binding.buttonSave.setOnClickListener(v -> saveProfile());
        binding.buttonChangePassword.setOnClickListener(v -> changePassword());
        binding.buttonLogout.setOnClickListener(v -> logout());
        
        // Load user data
        loadUserData();
    }

    private void loadUserData() {
        if (userId == null) return;
        
        profileManager.getCurrentUserProfile(new UserProfileManager.ProfileLoadListener() {
            @Override
            public void onProfileLoaded(User user) {
                if (getView() == null) return;
                
                binding.editName.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
                binding.editEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                // Phone field is not currently supported in the User model
                
                // Update UI based on the loaded data
                updateUIWithUserData(user);
            }

            @Override
            public void onError(String error) {
                showMessage("Failed to load profile: " + error);
            }
        });
    }
    
    private void updateUIWithUserData(User user) {
        if (getView() == null) return;
        
        // Enable/disable fields based on whether we have valid data
        binding.editName.setEnabled(true);
        binding.buttonSave.setEnabled(true);
    }

    private void saveProfile() {
        String displayName = binding.editName.getText().toString().trim();
        
        if (TextUtils.isEmpty(displayName)) {
            binding.layoutName.setError("Display name is required");
            return;
        }
        
        if (userId == null) return;
        
        showLoading(true);
        
        // Update user profile in Firebase Auth
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Update Firebase Auth profile
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build();
            
            user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    showLoading(false);
                    showMessage("Failed to update profile: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    return;
                }
                
                // Update user profile in database
                profileManager.createOrUpdateProfile(
                        userId, 
                        user.getEmail() != null ? user.getEmail() : "", 
                        displayName,
                        new UserProfileManager.ProfileUpdateListener() {
                            @Override
                            public void onSuccess() {
                                showLoading(false);
                                showMessage("Profile updated");
                            }

                            @Override
                            public void onError(String error) {
                                showLoading(false);
                                showMessage("Profile updated, but there was an error saving to database: " + error);
                            }
                        });
            });
        }
    }
    
    private void showLoading(boolean loading) {
        if (getView() == null) return;
        
        binding.buttonSave.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void changePassword() {
        // Implement password change logic
        showMessage("Password change functionality will be implemented here");
    }

    private void logout() {
        mAuth.signOut();
        // Navigate back to login/signup screen
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void showMessage(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
