package bootanimation.zip.bootzipcreator.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import bootanimation.zip.bootzipcreator.databinding.FragmentHelpBinding;

public class HelpFragment extends Fragment {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentHelpBinding binding = FragmentHelpBinding.inflate(inflater, container, false);
        binding.webView.loadUrl("file:///android_res/raw/tutorial.html");
        binding.webView.getSettings().setJavaScriptEnabled(true);
        return binding.getRoot();
    }
}