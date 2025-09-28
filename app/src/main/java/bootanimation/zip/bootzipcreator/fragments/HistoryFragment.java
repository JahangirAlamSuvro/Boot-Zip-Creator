package bootanimation.zip.bootzipcreator.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bootanimation.zip.bootzipcreator.R;
import bootanimation.zip.bootzipcreator.adapters.GeneratedAnimationsAdapter;
import bootanimation.zip.bootzipcreator.callbacks.BootAnimationCallback;
import bootanimation.zip.bootzipcreator.callbacks.DatabaseWriteCallback;
import bootanimation.zip.bootzipcreator.databinding.FragmentHistoryBinding;
import bootanimation.zip.bootzipcreator.dialogs.DialogExoPlayer;
import bootanimation.zip.bootzipcreator.dialogs.DialogHistoryItemContextMenu;
import bootanimation.zip.bootzipcreator.models.BootAnimation;
import bootanimation.zip.bootzipcreator.others.CustomMethods;
import bootanimation.zip.bootzipcreator.others.DatabaseHelper;


public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private GeneratedAnimationsAdapter animationsAdapter;
    private List<BootAnimation> bootAnimationList = new ArrayList<>();
    private DatabaseHelper dbHelper = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupSearch();
        loadAnimations();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            loadAnimations();
            binding.swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void setupRecyclerView() {
        animationsAdapter = new GeneratedAnimationsAdapter(requireActivity(), bootAnimationList);
        binding.generatedAnimationsRV.setAdapter(animationsAdapter);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 2);
        binding.generatedAnimationsRV.setLayoutManager(gridLayoutManager);

        animationsAdapter.setOnItemClickListener(new GeneratedAnimationsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String animationVideoPath = bootAnimationList.get(position).getVideoPath();
                Uri animationVideoUri = Uri.fromFile(new File(animationVideoPath));
                new DialogExoPlayer(requireActivity(), animationVideoUri);
            }

            @Override
            public void onItemLongClick(int position) {
                DialogHistoryItemContextMenu dialogHistoryItemContextMenu = new DialogHistoryItemContextMenu(requireContext(), bootAnimationList.get(position));
                dialogHistoryItemContextMenu.setOnActionListener(new DialogHistoryItemContextMenu.OnActionListener() {
                    @Override
                    public void onDelete(BootAnimation bootAnimation) {
                        if (dbHelper != null) {
                            dbHelper.deleteBootAnimationAsync(bootAnimation.getId(), new DatabaseWriteCallback() {
                                @SuppressLint("NotifyDataSetChanged")
                                @Override
                                public void onSuccess() {
                                    File zipFilePath = new File(bootAnimation.getZipPath());
                                    File videoFilePath = new File(bootAnimation.getVideoPath());

                                    if (zipFilePath.exists()) {
                                        zipFilePath.delete();
                                    }

                                    if (videoFilePath.exists()) {
                                        videoFilePath.delete();
                                    }
                                    Toast.makeText(requireContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
                                    loadAnimations();
                                }

                                @Override
                                public void onError(Exception e) {
                                    CustomMethods.showSimpleDialog(requireContext(), "Error", "Error: " + e.getLocalizedMessage(), R.drawable.baseline_error_24);
                                }
                            });
                        } else {
                            Toast.makeText(requireContext(), "DatabaseHelper is null", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onShare(BootAnimation bootAnimation) {

                        File file = new File(bootAnimation.getZipPath());
                        Uri fileUri = FileProvider.getUriForFile(
                                requireContext(),
                                requireContext().getPackageName() + ".provider",
                                file
                        );

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("*/*"); // or specific MIME type
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        requireContext().startActivity(Intent.createChooser(shareIntent, "Share file via"));

                    }
                });
                dialogHistoryItemContextMenu.show();
            }
        });
    }

    private void loadAnimations() {
        dbHelper = DatabaseHelper.getInstance(requireContext());

        dbHelper.getAllBootAnimationsAsync(new BootAnimationCallback() {
            @Override
            public void onComplete(List<BootAnimation> animations) {
                // Check if the fragment is still attached to the activity
                if (isAdded() && animationsAdapter != null) {
                    bootAnimationList = animations;
                    animationsAdapter.updateList(bootAnimationList);

                    if (bootAnimationList.isEmpty()) {
                        binding.emptyGenerationsTV.setVisibility(View.VISIBLE);
                        binding.generatedAnimationsRV.setVisibility(View.GONE);
                    } else {
                        binding.emptyGenerationsTV.setVisibility(View.GONE);
                        binding.generatedAnimationsRV.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    CustomMethods.showSimpleDialog(requireContext(), "Error", "Error: " + e.getLocalizedMessage(), R.drawable.baseline_error_24);
                }
            }
        });
    }

    private void setupSearch() {
        binding.searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (animationsAdapter != null) {
                    animationsAdapter.getFilter().filter(s);
                    binding.clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        binding.clearButton.setOnClickListener(v -> {
            binding.searchBox.setText("");
            binding.clearButton.setVisibility(View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

