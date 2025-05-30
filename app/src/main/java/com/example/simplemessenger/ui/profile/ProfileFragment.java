package com.example.simplemessenger.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.simplemessenger.R;
import com.example.simplemessenger.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
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
        
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String phone = dataSnapshot.child("phone").getValue(String.class);
                    
                    binding.editName.setText(name != null ? name : "");
                    binding.editEmail.setText(email != null ? email : "");
                    binding.editPhone.setText(phone != null ? phone : "");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showMessage("Failed to load profile: " + databaseError.getMessage());
            }
        });
    }

    private void saveProfile() {
        String name = binding.editName.getText().toString().trim();
        String phone = binding.editPhone.getText().toString().trim();
        
        if (name.isEmpty()) {
            binding.layoutName.setError("Name is required");
            return;
        }
        
        if (userId == null) return;
        
        // Update user data in database
        mDatabase.child("users").child(userId).child("name").setValue(name);
        mDatabase.child("users").child(userId).child("phone").setValue(phone)
                .addOnSuccessListener(aVoid -> showMessage("Profile updated"))
                .addOnFailureListener(e -> showMessage("Failed to update profile: " + e.getMessage()));
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
