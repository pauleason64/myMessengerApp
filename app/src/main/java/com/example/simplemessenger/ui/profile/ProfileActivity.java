package com.example.simplemessenger.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.simplemessenger.R;
import com.example.simplemessenger.databinding.ActivityProfileBinding;
import com.example.simplemessenger.ui.auth.AuthActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up toolbar
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_profile);
        }

        mAuth = FirebaseAuth.getInstance();
        loadUserProfile();

        // Set up click listeners
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.btnEditProfile.setOnClickListener(v -> editProfile());
        binding.btnSignOut.setOnClickListener(v -> signOut());
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            binding.tvEmail.setText(user.getEmail());
            binding.tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : getString(R.string.not_available));
        }
    }

    private void showChangePasswordDialog() {
        // TODO: Implement change password dialog
        Toast.makeText(this, "Change password clicked", Toast.LENGTH_SHORT).show();
    }

    private void editProfile() {
        // TODO: Implement edit profile
        Toast.makeText(this, "Edit profile clicked", Toast.LENGTH_SHORT).show();
    }

    private void signOut() {
        mAuth.signOut();
        startActivity(new Intent(this, AuthActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
