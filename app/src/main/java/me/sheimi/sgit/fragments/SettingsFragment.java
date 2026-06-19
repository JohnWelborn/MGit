package me.sheimi.sgit.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import androidx.core.app.TaskStackBuilder;

import java.io.File;

import me.sheimi.android.activities.SheimiFragmentActivity;
import me.sheimi.android.utils.BasicFunctions;
import me.sheimi.sgit.R;
import me.sheimi.sgit.activities.explorer.ExploreRootDirActivity;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.MGitApplication;
import com.manichord.mgit.permissions.PermissionsHelper;
import com.manichord.mgit.repolist.RepoListActivity;

public class SettingsFragment extends PreferenceFragment {
    private SharedPreferences.OnSharedPreferenceChangeListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // need to set as for historical reasons SGit uses custom prefs file
        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(getString(R.string.preference_file_key));
        prefMgr.setSharedPreferencesMode(Context.MODE_PRIVATE);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        final String themePrefKey = getString(R.string.pref_key_use_theme_id);
        final String gravatarPrefKey = getString(R.string.pref_key_use_gravatar);
        final String useEnglishPrefKey = getString(R.string.pref_key_use_english);

        Preference repoRootPref = findPreference(getString(R.string.pref_key_repo_root_location));
        updateRepoRootSummary(repoRootPref);
        updatePermissionNoticeVisibility();
        repoRootPref.setOnPreferenceClickListener(pref -> {
            SheimiFragmentActivity activity = (SheimiFragmentActivity) getActivity();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    && !PermissionsHelper.Companion.isExternalStorageManager()) {
                activity.checkAndRequestFullStoragePermission();
            }
            startActivity(new Intent(activity, ExploreRootDirActivity.class));
            return true;
        });

        mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (themePrefKey.equals(key) || useEnglishPrefKey.equals(key)) {
                    // nice trick to recreate the back stack, to ensure existing activities onCreate() are
                    // called to set new theme, courtesy of: http://stackoverflow.com/a/28799124/85472
                    TaskStackBuilder.create(getActivity())
                            .addNextIntent(new Intent(getActivity(), RepoListActivity.class))
                            .addNextIntent(getActivity().getIntent())
                            .startActivities();
                }
                else if (gravatarPrefKey.equals(key)) {
                    BasicFunctions.getImageLoader().clearMemoryCache();
                    BasicFunctions.getImageLoader().clearDiskCache();
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(mListener);
        updateRepoRootSummary(findPreference(getString(R.string.pref_key_repo_root_location)));
        updatePermissionNoticeVisibility();
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mListener);
    }

    private void updatePermissionNoticeVisibility() {
        Preference noticePref = findPreference("pref_key_repo_location_permission_notice");
        if (noticePref == null) return;
        boolean needsNotice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && !PermissionsHelper.Companion.isExternalStorageManager();
        if (!needsNotice) {
            PreferenceGroup parent = (PreferenceGroup) findPreference("pref_key_storage_root_location");
            if (parent != null) parent.removePreference(noticePref);
        }
    }

    private void updateRepoRootSummary(Preference pref) {
        MGitApplication app = (MGitApplication) getActivity().getApplicationContext();
        File root = app.getPrefenceHelper().getRepoRoot();
        if (root != null) {
            pref.setSummary(root.getAbsolutePath());
        } else {
            File defaultRoot = getActivity().getExternalFilesDir(null);
            pref.setSummary(defaultRoot != null
                    ? defaultRoot.getAbsolutePath() + "/" + Repo.REPO_DIR
                    : "");
        }
    }
}
