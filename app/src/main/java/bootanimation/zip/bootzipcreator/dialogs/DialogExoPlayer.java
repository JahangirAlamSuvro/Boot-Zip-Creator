package bootanimation.zip.bootzipcreator.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.net.Uri;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import bootanimation.zip.bootzipcreator.R;

public class DialogExoPlayer {

    private Dialog dialog;

    public DialogExoPlayer(Activity activity, Uri videoUri) {
        initializeDialog(activity, videoUri);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializeDialog(Activity activity, Uri videoUri) {
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_exoplayer);
        setupWindow();

        PlayerView dialogExoPlayerView = dialog.findViewById(R.id.dialogExoPlayerView);
        ExoPlayer player = new ExoPlayer.Builder(activity).build();
        player.setMediaItem(MediaItem.fromUri(videoUri));
        player.prepare();
        dialogExoPlayerView.setPlayer(player);
        player.setPlayWhenReady(true);

        // Hide controller when playback begins
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    dialogExoPlayerView.hideController(); // hide immediately
                }
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    // Show controller after video ends
                    dialogExoPlayerView.showController();
                }
            }
        });

        ImageButton closeDialogBtn = dialog.findViewById(R.id.closeDialogBtn);
        closeDialogBtn.setOnClickListener(v -> {
            if (dialog != null && dialog.isShowing()) {
                player.release();
                dialog.dismiss();
            }
        });

        dialog.setOnDismissListener(dialogInterface -> {
            player.release();
            dialog.dismiss();
        });

        if (dialog != null) {
            dialog.show();
        }
    }

    private void setupWindow() {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.white);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }
}
