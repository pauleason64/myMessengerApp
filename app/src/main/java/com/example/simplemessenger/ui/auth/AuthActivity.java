package com.example.simplemessenger.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.simplemessenger.R;
import com.example.simplemessenger.SimpleMessengerApp;
import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.ui.main.MainActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import com.example.simplemessenger.utils.AuthUtils;

import java.util.HashMap;
import java.util.Map;
public class AuthActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword, inputName, inputConfirmPassword;
    private TextInputLayout layoutName, layoutConfirmPassword;
    private CheckBox checkRememberMe, checkTerms;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private boolean isRegistering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Set up the Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(R.string.app_name);
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(
                SimpleMessengerApp.SHARED_PREFS_NAME, 
                Context.MODE_PRIVATE
        );

        // Initialize views
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        inputName = findViewById(R.id.input_name);
        inputConfirmPassword = findViewById(R.id.input_confirm_password);
        layoutName = findViewById(R.id.layout_name);
        layoutConfirmPassword = findViewById(R.id.layout_confirm_password);
        checkRememberMe = findViewById(R.id.check_remember_me);
        checkTerms = findViewById(R.id.check_terms);
        progressBar = findViewById(R.id.progress_bar);
        
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnRegister = findViewById(R.id.btn_register);
        Button btnRegisterSubmit = findViewById(R.id.btn_register_submit);
        TextView textForgotPassword = findViewById(R.id.text_forgot_password);
        TextView textLogin = findViewById(R.id.text_login);

        // Set click listeners
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> showRegistration());
        btnRegisterSubmit.setOnClickListener(v -> attemptRegistration());
        textForgotPassword.setOnClickListener(v -> resetPassword());
        textLogin.setOnClickListener(v -> showLogin());
        
        // Set up keyboard actions
        inputPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                attemptLogin();
                return true;
            }
            return false;
        });

        // Check if we should show login form directly
        checkAutoLogin();
    }


    private void checkAutoLogin() {
        boolean rememberMe = sharedPreferences.getBoolean(
                SimpleMessengerApp.PREF_REMEMBER_ME, 
                false
        );
        
        if (rememberMe) {
            // Only restore the email, don't auto-login
            String email = sharedPreferences.getString(SimpleMessengerApp.PREF_USER_EMAIL, "");
            if (!TextUtils.isEmpty(email)) {
                inputEmail.setText(email);
                checkRememberMe.setChecked(true);
                inputPassword.requestFocus();
            }
        }
    }

    private void attemptRegistration() {
        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();

        // Reset errors
        inputName.setError(null);
        inputEmail.setError(null);
        inputPassword.setError(null);
        inputConfirmPassword.setError(null);

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            inputName.setError(getString(R.string.error_field_required));
            inputName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            inputEmail.setError(getString(R.string.error_field_required));
            inputEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError(getString(R.string.error_invalid_email));
            inputEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            inputPassword.setError(getString(R.string.error_field_required));
            inputPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            inputPassword.setError(getString(R.string.error_invalid_password));
            inputPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError(getString(R.string.error_password_mismatch));
            inputConfirmPassword.requestFocus();
            return;
        }

        if (!checkTerms.isChecked()) {
            Toast.makeText(this, R.string.error_accept_terms, Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Send verification email
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            sendVerificationEmail(user, name, email);
                        }
                    } else {
                        showProgress(false);
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : getString(R.string.error_registration_failed);
                        Toast.makeText(AuthActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    private void sendVerificationEmail(FirebaseUser user, String name, String email) {
        user.sendEmailVerification()
            .addOnCompleteListener(task -> {
                showProgress(false);
                if (task.isSuccessful()) {
                    // Save user data with email not verified
                    saveUserToDatabase(user.getUid(), name, email, false);
                    // Show verification dialog
                    showVerificationDialog(user, email);
                } else {
                    Toast.makeText(
                        AuthActivity.this, 
                        getString(R.string.error_sending_verification_email), 
                        Toast.LENGTH_LONG
                    ).show();
                }
            });
    }
    
    private void showVerificationDialog(FirebaseUser user, String email) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.verification_email_sent_title)
            .setMessage(getString(R.string.verification_email_sent_message, email))
            .setPositiveButton(R.string.resend_verification, (dialog, which) -> {
                showProgress(true);
                sendVerificationEmail(user, user.getDisplayName(), email);
            })
            .setNegativeButton(R.string.dismiss, (dialog, which) -> {
                // Clear the form and switch back to login
                resetForm();
                showLogin();
            })
            .setCancelable(false)
            .show();
    }

    private void updateUserProfile(FirebaseUser user, String name) {
        UserProfileChangeRequest profileUpdates = 
                new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("AuthActivity", "Error updating user profile", task.getException());
                    }
                });
    }

    private void saveUserToDatabase(String userId, String name, String email, boolean isEmailVerified) {
        // Create a user map
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("emailVerified", isEmailVerified);
        user.put("createdAt", ServerValue.TIMESTAMP);
        user.put("lastLogin", ServerValue.TIMESTAMP);
        user.put("profileImageUrl", "");

        // Save to Realtime Database using DatabaseHelper
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance();
        databaseHelper.getDatabaseReference().child("users").child(userId)
                .setValue(user)
                .addOnSuccessListener(aVoid -> Log.d("AuthActivity", "User data saved successfully"))
                .addOnFailureListener(e -> Log.e("AuthActivity", "Error saving user data", e));
    }

    private void attemptLogin() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            inputEmail.setError(getString(R.string.error_field_required));
            inputEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            inputPassword.setError(getString(R.string.error_field_required));
            inputPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            inputPassword.setError(getString(R.string.error_invalid_password));
            inputPassword.requestFocus();
            return;
        }

        showProgress(true);
        
        // Authenticate with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                // Email is verified, proceed with login
                                handleSuccessfulLogin(user, email);
                            } else {
                                // Email not verified, show verification dialog
                                showEmailNotVerifiedDialog(user);
                            }
                        }
                    } else {
                        showProgress(false);
                        // If sign in fails, display a message to the user.
                        Toast.makeText(
                                AuthActivity.this,
                                task.getException() != null ? 
                                    task.getException().getMessage() : 
                                    getString(R.string.error_login_failed),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
    
    private void handleSuccessfulLogin(FirebaseUser user, String email) {
        try {
            if (user == null) {
                Log.e("AuthActivity", "User is null in handleSuccessfulLogin");
                showError("Error: User information not available. Please try again.");
                showProgress(false);
                return;
            }
            
            Log.d("AuthActivity", "Handling successful login for: " + email);
            
            // Get display name or use email prefix if display name is not set
            String displayName = user.getDisplayName();
            if (TextUtils.isEmpty(displayName)) {
                try {
                    displayName = email.split("@")[0];
                } catch (Exception e) {
                    Log.e("AuthActivity", "Error extracting username from email", e);
                    displayName = "User";
                }
            }
            
            // Update user data with verified status
            Log.d("AuthActivity", "Saving user to database: " + displayName);
            saveUserToDatabase(user.getUid(), displayName, email, true);
            
            // Save login state
            Log.d("AuthActivity", "Saving login state");
            saveLoginState(email);
            
            // Go to main activity
            Log.d("AuthActivity", "Starting MainActivity");
            startMainActivity();
            
        } catch (Exception e) {
            Log.e("AuthActivity", "Error in handleSuccessfulLogin", e);
            showError("An unexpected error occurred. Please try again.");
            showProgress(false);
        }
    }
    
    private void showEmailNotVerifiedDialog(FirebaseUser user) {
        showProgress(false);
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.email_not_verified_title)
            .setMessage(R.string.email_not_verified_message)
            .setPositiveButton(R.string.resend_verification, (dialog, which) -> {
                showProgress(true);
                sendVerificationEmail(user, user.getDisplayName(), user.getEmail());
            })
            .setNegativeButton(R.string.dismiss, null)
            .show();
    }

    private void saveLoginState(String email) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Save basic login state
            AuthUtils.saveLoginState(this, true, email, user.getUid());
            
            // Save additional user preferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(SimpleMessengerApp.PREF_USER_NAME, 
                    user.getDisplayName() != null ? user.getDisplayName() : email.split("@")[0]);
            editor.putBoolean(SimpleMessengerApp.PREF_REMEMBER_ME, checkRememberMe.isChecked());
            editor.apply();
        }
    }

    private void startMainActivity() {
        runOnUiThread(() -> {
            try {
                Log.d("AuthActivity", "Creating MainActivity intent");
                Intent intent = new Intent(this, MainActivity.class);
                
                // Clear any existing tasks and create a new task
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | 
                              Intent.FLAG_ACTIVITY_NEW_TASK | 
                              Intent.FLAG_ACTIVITY_CLEAR_TOP);
                
                Log.d("AuthActivity", "Starting MainActivity with flags: " + intent.getFlags());
                startActivity(intent);
                
                // Add a small delay to ensure the activity starts before finishing this one
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        Log.d("AuthActivity", "Finishing AuthActivity");
                        finishAffinity();
                    } catch (Exception e) {
                        Log.e("AuthActivity", "Error finishing activity", e);
                    }
                }, 100);
                
            } catch (Exception e) {
                Log.e("AuthActivity", "Error starting MainActivity", e);
                showError("Failed to start the application. Please try again.");
                showProgress(false);
            }
        });
    }

    private void showRegistration() {
        isRegistering = true;
        // Show registration fields
        layoutName.setVisibility(View.VISIBLE);
        layoutConfirmPassword.setVisibility(View.VISIBLE);
        checkTerms.setVisibility(View.VISIBLE);
        checkRememberMe.setVisibility(View.GONE);
        findViewById(R.id.btn_login).setVisibility(View.GONE);
        findViewById(R.id.btn_register).setVisibility(View.GONE);
        findViewById(R.id.text_forgot_password).setVisibility(View.GONE);
        findViewById(R.id.btn_register_submit).setVisibility(View.VISIBLE);
        findViewById(R.id.text_login).setVisibility(View.VISIBLE);
        
        // Update UI
        inputName.requestFocus();
    }
    
    private void showLogin() {
        isRegistering = false;
        // Hide registration fields
        layoutName.setVisibility(View.GONE);
        layoutConfirmPassword.setVisibility(View.GONE);
        checkTerms.setVisibility(View.GONE);
        checkRememberMe.setVisibility(View.VISIBLE);
        findViewById(R.id.btn_login).setVisibility(View.VISIBLE);
        findViewById(R.id.btn_register).setVisibility(View.VISIBLE);
        findViewById(R.id.text_forgot_password).setVisibility(View.VISIBLE);
        findViewById(R.id.btn_register_submit).setVisibility(View.GONE);
        findViewById(R.id.text_login).setVisibility(View.GONE);
    }

    private void resetPassword() {
        String email = inputEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            inputEmail.setError(getString(R.string.error_field_required));
            inputEmail.requestFocus();
            return;
        }

        showProgress(true);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showProgress(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(
                                AuthActivity.this,
                                R.string.reset_password_email_sent,
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        Toast.makeText(
                                AuthActivity.this,
                                R.string.error_reset_password_failed,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void showProgress(boolean show) {
        runOnUiThread(() -> progressBar.setVisibility(show ? View.VISIBLE : View.GONE));
    }
    
    private void showError(String message) {
        runOnUiThread(() -> {
            try {
                Log.d("AuthActivity", "Showing error: " + message);
                Toast.makeText(AuthActivity.this, message, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e("AuthActivity", "Error showing toast", e);
            }
        });
    }
    
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    
    private void resetForm() {
        inputName.setText("");
        inputEmail.setText("");
        inputPassword.setText("");
        inputConfirmPassword.setText("");
        inputName.setError(null);
        inputEmail.setError(null);
        inputPassword.setError(null);
        inputConfirmPassword.setError(null);
        checkTerms.setChecked(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any references to prevent memory leaks
        inputEmail = null;
        inputPassword = null;
        inputName = null;
        inputConfirmPassword = null;
        checkRememberMe = null;
        checkTerms = null;
        progressBar = null;
        mAuth = null;
        sharedPreferences = null;
    }
}
