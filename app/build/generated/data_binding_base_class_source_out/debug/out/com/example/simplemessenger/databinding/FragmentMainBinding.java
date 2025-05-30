// Generated by view binder compiler. Do not edit!
package com.example.simplemessenger.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import androidx.viewpager.widget.ViewPager;
import com.example.simplemessenger.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentMainBinding implements ViewBinding {
  @NonNull
  private final CoordinatorLayout rootView;

  @NonNull
  public final ViewPager container;

  @NonNull
  public final RadioGroup radioGroup;

  @NonNull
  public final RadioButton radioInbox;

  @NonNull
  public final RadioButton radioOutbox;

  @NonNull
  public final Toolbar toolbar;

  private FragmentMainBinding(@NonNull CoordinatorLayout rootView, @NonNull ViewPager container,
      @NonNull RadioGroup radioGroup, @NonNull RadioButton radioInbox,
      @NonNull RadioButton radioOutbox, @NonNull Toolbar toolbar) {
    this.rootView = rootView;
    this.container = container;
    this.radioGroup = radioGroup;
    this.radioInbox = radioInbox;
    this.radioOutbox = radioOutbox;
    this.toolbar = toolbar;
  }

  @Override
  @NonNull
  public CoordinatorLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentMainBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentMainBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_main, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentMainBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.container;
      ViewPager container = ViewBindings.findChildViewById(rootView, id);
      if (container == null) {
        break missingId;
      }

      id = R.id.radio_group;
      RadioGroup radioGroup = ViewBindings.findChildViewById(rootView, id);
      if (radioGroup == null) {
        break missingId;
      }

      id = R.id.radio_inbox;
      RadioButton radioInbox = ViewBindings.findChildViewById(rootView, id);
      if (radioInbox == null) {
        break missingId;
      }

      id = R.id.radio_outbox;
      RadioButton radioOutbox = ViewBindings.findChildViewById(rootView, id);
      if (radioOutbox == null) {
        break missingId;
      }

      id = R.id.toolbar;
      Toolbar toolbar = ViewBindings.findChildViewById(rootView, id);
      if (toolbar == null) {
        break missingId;
      }

      return new FragmentMainBinding((CoordinatorLayout) rootView, container, radioGroup,
          radioInbox, radioOutbox, toolbar);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
