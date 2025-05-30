package com.example.simplemessenger.ui.contacts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.simplemessenger.R;
import com.example.simplemessenger.databinding.FragmentContactsBinding;

public class ContactsFragment extends Fragment {

    public static ContactsFragment newInstance() {
        return new ContactsFragment();
    }

    private FragmentContactsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        binding = FragmentContactsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Add ContactsListFragment to the container
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ContactsListFragment())
                .commit();

        // Set up FAB
        binding.fabAddContact.setOnClickListener(v -> showAddContactDialog());
    }

    
    private void showAddContactDialog() {
        // Implement add contact dialog
        // This will be called when the FAB is clicked
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}
