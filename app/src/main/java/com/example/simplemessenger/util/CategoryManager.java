package com.example.SImpleMessenger.util;

import android.content.Context;
import android.util.Log;

import com.example.SImpleMessenger.data.DatabaseHelper;
import com.example.SImpleMessenger.data.DatabaseHelper.DatabaseCallback;
import com.example.SImpleMessenger.data.model.Category;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to manage message categories
 */
public class CategoryManager {
    private static final String TAG = "CategoryManager";
    private static CategoryManager instance;
    private final Map<String, Category> categoryCache = new HashMap<>();
    private final Context context;
    private final DatabaseHelper databaseHelper;

    private CategoryManager(Context context) {
        this.context = context.getApplicationContext();
        this.databaseHelper = DatabaseHelper.getInstance();
        loadDefaultCategories();
    }

    public static synchronized CategoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new CategoryManager(context);
        }
        return instance;
    }

    /**
     * Load default categories from the raw resource file
     */
    private void loadDefaultCategories() {
        try {
            // Read the JSON file from assets
            InputStream inputStream = context.getAssets().open("defaultCatagories.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            
            // Parse the JSON
            String json = stringBuilder.toString();
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
            Map<String, List<String>> jsonMap = gson.fromJson(json, type);
            
            if (jsonMap != null && jsonMap.containsKey("categories")) {
                List<String> categoryNames = jsonMap.get("categories");
                for (String name : categoryNames) {
                    // Use the category name as both ID and name for now
                    Category category = new Category(name.toLowerCase().replace(" ", "_"), name);
                    categoryCache.put(category.getId(), category);
                }
                Log.d(TAG, "Loaded " + categoryNames.size() + " default categories");
            }
            
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error loading default categories", e);
        }
    }

    /**
     * Get all categories
     * @return List of all categories
     */
    public List<Category> getAllCategories() {
        return new ArrayList<>(categoryCache.values());
    }

    /**
     * Get category by ID
     * @param id Category ID
     * @return Category or null if not found
     */
    public Category getCategory(String id) {
        return categoryCache.get(id);
    }

    /**
     * Get the default category (Reminder)
     * @return Default category
     */
    public Category getDefaultCategory() {
        // Return "Reminder" category or the first one if not found
        Category reminder = categoryCache.get("reminder");
        return reminder != null ? reminder : 
               (categoryCache.isEmpty() ? null : categoryCache.values().iterator().next());
    }
    
    /**
     * Save default categories for a new user
     * @param userId The ID of the user
     * @param listener Callback for success/failure
     */
    public void saveDefaultCategoriesForNewUser(String userId, CategoryUpdateListener listener) {
        if (userId == null || userId.isEmpty()) {
            if (listener != null) {
                listener.onError("Invalid user ID");
            }
            return;
        }

        // Convert categories to a format that can be saved
        List<Map<String, Object>> categoriesToSave = new ArrayList<>();
        for (Category category : categoryCache.values()) {
            Map<String, Object> categoryMap = new HashMap<>();
            categoryMap.put("id", category.getId());
            categoryMap.put("name", category.getName());
            categoriesToSave.add(categoryMap);
        }

        // Save using DatabaseHelper
        databaseHelper.saveUserCategories(userId, categoriesToSave, new DatabaseHelper.DatabaseCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Default categories saved for user: " + userId);
                if (listener != null) {
                    listener.onSuccess();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error saving default categories: " + error);
                if (listener != null) {
                    listener.onError(error);
                }
            }
        });
    }

    /**
     * Check if a user has categories saved
     * @param userId The ID of the user
     * @param listener Callback with the result
     */
    public void checkUserCategoriesExist(String userId, CategoryCheckListener listener) {
        if (userId == null || userId.isEmpty()) {
            if (listener != null) {
                listener.onResult(false);
            }
            return;
        }

        databaseHelper.checkUserCategoriesExist(userId, new DatabaseCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Categories exist for user: " + userId);
                if (listener != null) {
                    listener.onResult(true);
                }
            }

            @Override
            public void onError(String error) {
                Log.d(TAG, "No categories found for user: " + userId);
                if (listener != null) {
                    listener.onResult(false);
                }
            }
        });
    }

    public interface CategoryUpdateListener extends DatabaseCallback {
        void onSuccess();
    }

    public interface CategoryCheckListener {
        void onResult(boolean categoriesExist);
    }
}
