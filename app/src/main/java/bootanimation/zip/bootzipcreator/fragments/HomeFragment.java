package bootanimation.zip.bootzipcreator.fragments;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import bootanimation.zip.bootzipcreator.R;
import bootanimation.zip.bootzipcreator.databinding.FragmentHomeBinding;
import bootanimation.zip.bootzipcreator.dialogs.DialogExoPlayer;
import bootanimation.zip.bootzipcreator.dialogs.DialogProcessingVideo;
import bootanimation.zip.bootzipcreator.dialogs.DialogTakeWidthHeight;
import bootanimation.zip.bootzipcreator.others.CustomMethods;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ActivityResultLauncher<String[]> videoPickerLauncher;
    private ExoPlayer player;

    private Handler loopHandler;
    private Runnable loopRunnable;
    private float videoFps = 0f;
    private long startMs = 0;
    private long endMs = 0;
    private boolean isPlayerInitialized = false;
    private boolean isLoopActive = false;
    private int videoWidth = 0;
    private int videoHeight = 0;
    private int totalFrames = 0;
    private Uri selectedVideoUri;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        videoPickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {

            if (uri != null) {
                selectedVideoUri = uri; // Store the selected URI
                if (player != null) {
                    stopLooping();
                    player.release();
                }

                isPlayerInitialized = false;
                isLoopActive = false;

                initializeVideoPlayer(uri, (isSupported, frames) -> {
                    if (isSupported) {
                        this.totalFrames = frames; // Store total frames

                        binding.blankVideoImageView.setVisibility(View.GONE);
                        binding.exoPlayerView.setVisibility(View.VISIBLE);
                        binding.exoPlayerView.setPlayer(player);
                        binding.startBtn.setEnabled(true);
                        binding.videoPlayPauseBtn.setVisibility(View.VISIBLE);
                        binding.trimSlider.setVisibility(View.VISIBLE);

                        binding.videoPlayPauseBtn.setOnClickListener(view -> {
                            if (player.isPlaying()) {
                                player.pause();
                                isLoopActive = false;
                                binding.videoPlayPauseBtn.setImageResource(R.drawable.baseline_play_circle_filled_24);
                                stopLooping();
                            } else {
                                player.play();
                                isLoopActive = true;
                                binding.videoPlayPauseBtn.setImageResource(R.drawable.baseline_pause_circle_filled_24);
                                startLooping();
                            }
                        });

                        binding.videoPlayPauseBtn.setOnLongClickListener(view -> {
                            if (player.isPlaying()) {
                                player.pause();
                                binding.videoPlayPauseBtn.setImageResource(R.drawable.baseline_play_circle_filled_24);
                                stopLooping();
                            }
                            new DialogExoPlayer(requireActivity(), uri);
                            return true;
                        });

                        binding.trimSlider.addOnChangeListener((slider, value, fromUser) -> {
                            if (fromUser && videoFps > 0) {
                                float startFrame = slider.getValues().get(0);
                                float endFrame = slider.getValues().get(1);
                                startMs = (long) ((startFrame / videoFps) * 1000);
                                endMs = (long) ((endFrame / videoFps) * 1000);
                                player.seekTo(startMs);
                            }
                        });

                    } else {
                        Toast.makeText(getContext(), "Video is not supported.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getContext(), "No video selected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        binding.selectVideoBtn.setOnClickListener(view -> videoPickerLauncher.launch(new String[]{"video/*"}));

        binding.startBtn.setOnClickListener(view -> {
            if (!isPlayerInitialized || selectedVideoUri == null) {
                Toast.makeText(requireContext(), "Please select a valid video first.", Toast.LENGTH_SHORT).show();
                return;
            }

            float loopStartFrame =  binding.trimSlider.getValues().get(0);
            float loopEndFrame = binding.trimSlider.getValues().get(1);

            if (loopEndFrame - loopStartFrame < 2) {
                Toast.makeText(requireContext(), "Please select a valid range.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (videoFps <= 0) {
                Toast.makeText(requireContext(), "Video is not supported.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (totalFrames <= 0) {
                Toast.makeText(requireContext(), "Video is not supported.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (totalFrames <= loopStartFrame || totalFrames <= loopEndFrame) {
                Toast.makeText(requireContext(), "Please select a valid range.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use device resolution as default if video resolution is not available
            if (videoWidth == 0 || videoHeight == 0) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                videoWidth = displayMetrics.widthPixels;
                videoHeight = displayMetrics.heightPixels;
            }

            DialogTakeWidthHeight takeWidthHeightDialog = new DialogTakeWidthHeight(requireActivity(), new DialogTakeWidthHeight.OnOkayClickListener() {
                @Override
                public void onOkayClick(int width, int height) {
                    videoWidth = width;
                    videoHeight = height;

                    // Pause the video if it's playing
                    if (player != null && player.isPlaying()) {
                        player.pause();
                        binding.videoPlayPauseBtn.setImageResource(R.drawable.baseline_play_circle_filled_24);
                        stopLooping();
                    }

                    // Proceed to processing dialog
                    DialogProcessingVideo processingDialog = new DialogProcessingVideo(requireActivity());
                    processingDialog.setVideoUri(selectedVideoUri);
                    processingDialog.setVideoInfo(videoWidth, videoHeight, (int) videoFps, totalFrames);
                    processingDialog.setTrimRange(binding.trimSlider.getValues().get(0), binding.trimSlider.getValues().get(1));
                    processingDialog.startProcessing();
                }

                @Override
                public void onCancelClick(Dialog dialog) {
                    dialog.dismiss();
                }
            });
            takeWidthHeightDialog.setDefaultValues(videoWidth, videoHeight);
            takeWidthHeightDialog.show();
        });


        initializeLoopingHandler();
        return binding.getRoot();
    }

    private void initializeLoopingHandler() {
        loopHandler = new Handler(Looper.getMainLooper());
        loopRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && isLoopActive) {
                    long currentPosition = player.getCurrentPosition();
                    if (currentPosition >= endMs || player.getPlaybackState() == Player.STATE_ENDED) {
                        player.seekTo(startMs);
                    }
                    loopHandler.postDelayed(this, 100);
                }
            }
        };
    }

    private void startLooping() {
        if (loopHandler != null && loopRunnable != null) {
            loopHandler.removeCallbacks(loopRunnable);
            loopHandler.post(loopRunnable);
        }
    }

    private void stopLooping() {
        isLoopActive = false;
        if (loopHandler != null && loopRunnable != null) {
            loopHandler.removeCallbacks(loopRunnable);
        }
    }

    private void initializeVideoPlayer(Uri videoUri, VideoSupportCallback callback) {
        MediaItem videoItem = MediaItem.fromUri(videoUri);
        player = new ExoPlayer.Builder(requireContext()).build();
        player.setMediaItem(videoItem);
        player.prepare();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Player.Listener.super.onPlayerError(error);
                callback.isVideoSupported(false, 0);
            }

            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY && !isPlayerInitialized) {
                    Format videoFormat = player.getVideoFormat();
                    if (videoFormat != null) {
                        videoFps = videoFormat.frameRate;
                        long durationMs = player.getDuration();
                        startMs = 0;
                        endMs = durationMs;

                        float durationSeconds = durationMs / 1000f;
                        int totalFrames = Math.round(durationSeconds * videoFps);

                        if (durationSeconds > 60) {
                            CustomMethods.showSimpleDialog(requireContext(), "Error", "Video is too long. Must be less than 60 seconds", R.drawable.baseline_error_24);
                            callback.isVideoSupported(false, 0);
                            return;
                        }

                        if (totalFrames <= 0) {
                            CustomMethods.showSimpleDialog(requireContext(), "Error", "Failed to get video frames. Please try another video.", R.drawable.baseline_error_24);
                            callback.isVideoSupported(false, 0);
                            return;
                        }

                        binding.trimSlider.setValueFrom(0);
                        binding.trimSlider.setValueTo(totalFrames);
                        binding.trimSlider.setStepSize(1);
                        binding.trimSlider.setValues(0f, (float) totalFrames);

                        callback.isVideoSupported(true, totalFrames);
                        isPlayerInitialized = true;
                    } else {
                        callback.isVideoSupported(false, 0);
                    }
                }
            }

            @Override
            public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
                videoWidth = videoSize.width;
                videoHeight = videoSize.height;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null && player.isPlaying()) {
            player.pause();
            binding.videoPlayPauseBtn.setImageResource(R.drawable.baseline_play_circle_filled_24);
        }
        stopLooping();
    }

    @Override
    public void onDestroy() {
        stopLooping();
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    private interface VideoSupportCallback {
        void isVideoSupported(boolean isSupported, int totalFrames);
    }
}
