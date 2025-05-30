package com.example.simplemessenger.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.simplemessenger.R;
import com.example.simplemessenger.databinding.FragmentSettingsBinding;
import com.example.simplemessenger.ui.auth.AuthActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private FirebaseAuth mAuth;
    private int currentTheme = AppCompatDelegate.MODE_NIGHT_NO;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        
        // Get current theme mode
        currentTheme = AppCompatDelegate.getDefaultNightMode();
        if (currentTheme == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
            currentTheme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set up click listeners
        setupClickListeners();
        
        // Set current theme text
        updateThemeText();
    }

    private void setupClickListeners() {
        // Theme selection
        binding.layoutTheme.setOnClickListener(v -> showThemeDialog());
        
        // Language selection
        binding.layoutLanguage.setOnClickListener(v -> {
            // Implement language selection
            Toast.makeText(requireContext(), "Language selection will be implemented here", Toast.LENGTH_SHORT).show();
        });
        
        // Help center
        binding.layoutHelp.setOnClickListener(v -> {
            // Implement help center
            Toast.makeText(requireContext(), "Help center will be implemented here", Toast.LENGTH_SHORT).show();
        });
        
        // About app
        binding.layoutAbout.setOnClickListener(v -> {
            // Show about dialog
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.about_app)
                    .setMessage(R.string.app_description)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
        
        // Logout
        binding.buttonLogout.setOnClickListener(v -> confirmLogout());
    }
    
    private void showThemeDialog() {
        String[] themes = {
                getString(R.string.theme_light),
                getString(R.string.theme_dark),
                getString(R.string.theme_system_default)
        };
        
        int checkedItem = 0; // Default to light theme
        if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            checkedItem = 1;
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            checkedItem = 2;
        }
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.choose_theme)
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            currentTheme = AppCompatDelegate.MODE_NIGHT_NO;
                            break;
                        case 1:
                            currentTheme = AppCompatDelegate.MODE_NIGHT_YES;
                            break;
                        case 2:
                            currentTheme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                            break;
                    }
                    dialog.dismiss();
                    applyTheme();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    
    private void applyTheme() {
        AppCompatDelegate.setDefaultNightMode(currentTheme);
        updateThemeText();
        // Restart activity to apply theme changes
        requireActivity().recreate();
    }
    
    private void updateThemeText() {
        String themeText;
        switch (currentTheme) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                themeText = getString(R.string.theme_light);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                themeText = getString(R.string.theme_dark);
                break;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                themeText = getString(R.string.theme_system_default);
                break;
            default:
                themeText = getString(R.string.theme_light);
        }
        binding.textTheme.setText(themeText);
    }
    
    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.are_you_sure_logout)
                .setPositiveButton(R.string.logout, (dialog, which) -> logout())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    
    private void logout() {
        mAuth.signOut();
        // Navigate to auth activity and clear back stack
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
