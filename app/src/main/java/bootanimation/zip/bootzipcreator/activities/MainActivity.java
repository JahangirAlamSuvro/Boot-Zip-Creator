package bootanimation.zip.bootzipcreator.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import bootanimation.zip.bootzipcreator.R;
import bootanimation.zip.bootzipcreator.databinding.ActivityMainBinding;
import bootanimation.zip.bootzipcreator.fragments.HelpFragment;
import bootanimation.zip.bootzipcreator.fragments.HistoryFragment;
import bootanimation.zip.bootzipcreator.fragments.HomeFragment;
import bootanimation.zip.bootzipcreator.others.CustomMethods;

public class MainActivity extends AppCompatActivity {

    private final HomeFragment homeFragment = new HomeFragment();
    private final HistoryFragment historyFragment = new HistoryFragment();
    private final HelpFragment helpFragment = new HelpFragment();
    private Fragment activeFragment = homeFragment;
    private final String TAG = "MainActivity";
    private AppUpdateManager appUpdateManager;
    private static final int UPDATE_REQUEST_CODE = 123;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // In-App Update >> Initialize and check for updates
        appUpdateManager = AppUpdateManagerFactory.create(this);
        checkForUpdate();

        //Add all fragments and hide the non-active ones
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().add(R.id.container, helpFragment, "3").hide(helpFragment).commit();
        fm.beginTransaction().add(R.id.container, historyFragment, "2").hide(historyFragment).commit();
        fm.beginTransaction().add(R.id.container, homeFragment, "1").commit();

        // Listener to use show/hide
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.home) {
                fm.beginTransaction().hide(activeFragment).show(homeFragment).commit();
                activeFragment = homeFragment;
            } else if (item.getItemId() == R.id.generated_files) {
                fm.beginTransaction().hide(activeFragment).show(historyFragment).commit();
                activeFragment = historyFragment;
            } else if (item.getItemId() == R.id.help) {
                fm.beginTransaction().hide(activeFragment).show(helpFragment).commit();
                activeFragment = helpFragment;
            }
            return true;
        });

        // Navigation Drawer------------------------------------------------------------------------
        binding.toolbar.menuBtn.setOnClickListener(view -> binding.drawerLayout.openDrawer(GravityCompat.START));
        View headView = binding.navigationView.getHeaderView(0);
        ((TextView) headView.findViewById(R.id.headerLayoutVersionTV)).setText("Version: " + CustomMethods.getVersionName(MainActivity.this));

        // Navigation Drawer Options Action
        binding.navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.report_bug_action) {
                String url = "https://wa.me/+917001952179?text=" + getString(R.string.app_name) + " (Bug Report)";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } else if (item.getItemId() == R.id.rate_action) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
            } else if (item.getItemId() == R.id.share_action) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                String shareMessage = "\nLet me recommend you this application\n\n";
                shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + getPackageName() + "\n\n";
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(shareIntent, "choose one"));
            } else if (item.getItemId() == R.id.privacy_policy_action) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://jahangiralamsuvro.github.io/Boot-Zip-Creator/pages/privacy-and-policy.html")));
            } else if (item.getItemId() == R.id.terms_of_use_action) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://jahangiralamsuvro.github.io/Boot-Zip-Creator/pages/terms-and-conditions.html")));
            }
            return true;
        });
    }

    // In-App Update >> Check if an update is available
    private void checkForUpdate() {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // An update is available. Prioritize IMMEDIATE update.
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    requestImmediateUpdate(appUpdateInfo);
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    // Fallback to FLEXIBLE update if IMMEDIATE is not available
                    appUpdateManager.registerListener(installStateUpdatedListener);
                    requestFlexibleUpdate(appUpdateInfo);
                }
            } else {
                Log.d("InAppUpdate", "No update available.");
            }
        });
    }

    // In-App Update >> Request an IMMEDIATE update
    private void requestImmediateUpdate(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    UPDATE_REQUEST_CODE);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "In-app update failed", e);
        }
    }

    // In-App Update >> Request a FLEXIBLE update
    private void requestFlexibleUpdate(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    this,
                    UPDATE_REQUEST_CODE);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "In-app update failed", e);
        }
    }

    // In-App Update >> Handle the result of the update flow
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Log.e("InAppUpdate", "Update flow failed! Result code: " + resultCode);
                // I can optionally retry the update here.
            }
        }
    }

    // In-App Update >> Resume an IMMEDIATE update that was interrupted
    @Override
    protected void onResume() {
        super.onResume();
        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(
                        appUpdateInfo -> {
                            if (appUpdateInfo.updateAvailability()
                                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                                // If an immediate update is in progress, resume it.
                                requestImmediateUpdate(appUpdateInfo);
                            }
                        });
    }

    // In-App Update >> Listener for FLEXIBLE update download status
    private final InstallStateUpdatedListener installStateUpdatedListener = state -> {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            popupSnackbarForCompleteUpdate();
        }
    };

    // In-App Update >> Show a Snackbar to prompt the user to restart for a FLEXIBLE update
    private void popupSnackbarForCompleteUpdate() {
        Snackbar.make(
                        findViewById(R.id.main),
                        "An update has just been downloaded.",
                        Snackbar.LENGTH_INDEFINITE)
                .setAction("RESTART", view -> {
                    if (appUpdateManager != null) {
                        appUpdateManager.completeUpdate();
                    }
                })
                .show();
    }

    // In-App Update >> Unregister the listener to prevent memory leaks
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // For FLEXIBLE updates, it's good practice to unregister the listener
        appUpdateManager.unregisterListener(installStateUpdatedListener);
    }
}