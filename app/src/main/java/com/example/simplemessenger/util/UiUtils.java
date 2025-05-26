package com.example.simplemessenger.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.simplemessenger.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * Utility class for common UI operations.
 */
public class UiUtils {

    private static final int SNACKBAR_DURATION = Snackbar.LENGTH_LONG;
    private static final int TOAST_DURATION = Toast.LENGTH_SHORT;

    // Prevent instantiation
    private UiUtils() {}

    /**
     * Shows a short toast message.
     *
     * @param context The context.
     * @param message The message to show.
     */
    public static void showToast(@NonNull Context context, @NonNull String message) {
        if (TextUtils.isEmpty(message)) return;
        Toast.makeText(context, message, TOAST_DURATION).show();
    }

    /**
     * Shows a short toast message.
     *
     * @param context The context.
     * @param resId   The string resource ID of the message to show.
     */
    public static void showToast(@NonNull Context context, @StringRes int resId) {
        showToast(context, context.getString(resId));
    }

    /**
     * Shows a long toast message.
     *
     * @param context The context.
     * @param message The message to show.
     */
    public static void showLongToast(@NonNull Context context, @NonNull String message) {
        if (TextUtils.isEmpty(message)) return;
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a long toast message.
     *
     * @param context The context.
     * @param resId   The string resource ID of the message to show.
     */
    public static void showLongToast(@NonNull Context context, @StringRes int resId) {
        showLongToast(context, context.getString(resId));
    }

    /**
     * Shows a snackbar with the given message.
     *
     * @param view    The view to find a parent from.
     * @param message The message to show.
     */
    public static void showSnackbar(@NonNull View view, @NonNull String message) {
        if (TextUtils.isEmpty(message)) return;
        Snackbar.make(view, message, SNACKBAR_DURATION).show();
    }

    /**
     * Shows a snackbar with the given message and action.
     *
     * @param view           The view to find a parent from.
     * @param message        The message to show.
     * @param actionText     The text of the action item.
     * @param onClickListener The callback to be invoked when the action is clicked.
     */
    public static void showSnackbarWithAction(@NonNull View view, @NonNull String message,
                                             @NonNull String actionText,
                                             @NonNull View.OnClickListener onClickListener) {
        if (TextUtils.isEmpty(message) || TextUtils.isEmpty(actionText)) return;
        Snackbar.make(view, message, SNACKBAR_DURATION)
                .setAction(actionText, onClickListener)
                .show();
    }

    /**
     * Shows a material alert dialog with the given title and message.
     *
     * @param context       The context.
     * @param title         The title of the dialog.
     * @param message       The message to show.
     * @param positiveText  The text of the positive button.
     * @param positiveClick The callback to be invoked when the positive button is clicked.
     * @param negativeText  The text of the negative button (can be null to hide).
     * @param negativeClick The callback to be invoked when the negative button is clicked.
     * @param cancelable    Whether the dialog is cancelable.
     * @return The created AlertDialog.
     */
    public static AlertDialog showAlertDialog(@NonNull Context context,
                                             @Nullable String title,
                                             @Nullable String message,
                                             @NonNull String positiveText,
                                             @Nullable DialogInterface.OnClickListener positiveClick,
                                             @Nullable String negativeText,
                                             @Nullable DialogInterface.OnClickListener negativeClick,
                                             boolean cancelable) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, positiveClick)
                .setCancelable(cancelable);

        if (negativeText != null) {
            builder.setNegativeButton(negativeText, negativeClick);
        }

        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    /**
     * Shows a progress dialog with the given message.
     *
     * @param activity The activity.
     * @param message  The message to show (can be null to use default).
     * @return The created AlertDialog.
     */
    public static AlertDialog showProgressDialog(@NonNull Activity activity, @Nullable String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_progress, null);
        
        TextView messageText = view.findViewById(R.id.text_message);
        if (message != null) {
            messageText.setText(message);
            messageText.setVisibility(View.VISIBLE);
        } else {
            messageText.setVisibility(View.GONE);
        }
        
        builder.setView(view);
        builder.setCancelable(false);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        dialog.show();
        return dialog;
    }

    /**
     * Sets the visibility of a view to VISIBLE.
     *
     * @param view The view to make visible.
     */
    public static void showView(@Nullable View view) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets the visibility of a view to GONE.
     *
     * @param view The view to hide.
     */
    public static void hideView(@Nullable View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the visibility of a view to INVISIBLE.
     *
     * @param view The view to make invisible.
     */
    public static void invisibleView(@Nullable View view) {
        if (view != null) {
            view.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Loads an image into an ImageView using Glide with default options.
     *
     * @param context   The context.
     * @param imageUrl  The URL of the image to load.
     * @param imageView The ImageView to load the image into.
     * @param placeholderResId The placeholder drawable resource ID.
     * @param errorResId      The error drawable resource ID.
     */
    public static void loadImage(@NonNull Context context, @Nullable String imageUrl,
                                @NonNull ImageView imageView,
                                @DrawableRes int placeholderResId,
                                @DrawableRes int errorResId) {
        if (TextUtils.isEmpty(imageUrl)) {
            imageView.setImageResource(placeholderResId);
            return;
        }

        RequestOptions options = new RequestOptions()
                .placeholder(placeholderResId)
                .error(errorResId)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop();

        Glide.with(context)
                .load(imageUrl)
                .apply(options)
                .into(imageView);
    }

    /**
     * Loads a circular image into an ImageView using Glide.
     *
     * @param context   The context.
     * @param imageUrl  The URL of the image to load.
     * @param imageView The ImageView to load the image into.
     * @param placeholderResId The placeholder drawable resource ID.
     * @param errorResId      The error drawable resource ID.
     */
    public static void loadCircularImage(@NonNull Context context, @Nullable String imageUrl,
                                        @NonNull ImageView imageView,
                                        @DrawableRes int placeholderResId,
                                        @DrawableRes int errorResId) {
        if (TextUtils.isEmpty(imageUrl)) {
            imageView.setImageResource(placeholderResId);
            return;
        }

        RequestOptions options = new RequestOptions()
                .placeholder(placeholderResId)
                .error(errorResId)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop();

        Glide.with(context)
                .load(imageUrl)
                .apply(options)
                .into(imageView);
    }

    /**
     * Shows the soft keyboard for the given view.
     *
     * @param view The view that should receive focus and show the keyboard.
     */
    public static void showSoftKeyboard(@NonNull View view) {
        if (view.requestFocus()) {
            view.postDelayed(() -> {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }, 100);
        }
    }

    /**
     * Hides the soft keyboard from the given view.
     *
     * @param view The view that currently has focus.
     */
    public static void hideSoftKeyboard(@NonNull View view) {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Sets the window to be displayed in fullscreen mode.
     *
     * @param window The window to modify.
     */
    public static void setFullScreen(@NonNull Window window) {
        window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
    }

    /**
     * Sets the status bar color of the window.
     *
     * @param window The window to modify.
     * @param color  The color to set.
     */
    public static void setStatusBarColor(@NonNull Window window, int color) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(color);
    }

    /**
     * Sets the navigation bar color of the window.
     *
     * @param window The window to modify.
     * @param color  The color to set.
     */
    public static void setNavigationBarColor(@NonNull Window window, int color) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(color);
    }

    /**
     * Sets the status bar icons to dark or light based on the background color.
     *
     * @param window       The window to modify.
     * @param isLightIcons Whether the status bar icons should be light (for dark backgrounds).
     */
    public static void setLightStatusBar(@NonNull Window window, boolean isLightIcons) {
        View decorView = window.getDecorView();
        int flags = decorView.getSystemUiVisibility();
        if (isLightIcons) {
            // Light status bar with dark icons
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            // Dark status bar with light icons
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        decorView.setSystemUiVisibility(flags);
    }

    /**
     * Sets the navigation bar icons to dark or light based on the background color.
     *
     * @param window       The window to modify.
     * @param isLightIcons Whether the navigation bar icons should be light (for dark backgrounds).
     */
    public static void setLightNavigationBar(@NonNull Window window, boolean isLightIcons) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            if (isLightIcons) {
                // Light navigation bar with dark icons
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                // Dark navigation bar with light icons
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decorView.setSystemUiVisibility(flags);
        }
    }
}
