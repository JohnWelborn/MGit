package me.sheimi.sgit.activities.explorer;

import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import java.io.File;
import java.io.FileFilter;

import com.manichord.mgit.permissions.PermissionsHelper;
import me.sheimi.sgit.R;
import me.sheimi.sgit.database.models.Repo;

public class ExploreRootDirActivity extends FileExplorerActivity {



    @Override
    protected File getRootFolder() {
        return Environment.getExternalStorageDirectory();
    }

    @Override
    protected FileFilter getExplorerFileFilter() {
        return new FileFilter() {
            @Override
            public boolean accept(File file) {
                String filename = file.getName();
                return !filename.startsWith(".") && file.isDirectory();
            }
        };
    }

    @Override
    protected AdapterView.OnItemClickListener getOnListItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                    int position, long id) {
                File file = mFilesListAdapter.getItem(position);
                if (file.isDirectory()) {
                    setCurrentDir(file);
                    return;
                }
            }
        };
    }

    @Override
    protected AdapterView.OnItemLongClickListener getOnListItemLongClickListener() {
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.select_root, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select_root:
                File selectedDir = getCurrentDir();
                Repo.setLocalRepoRoot(this, selectedDir);
                if (PermissionsHelper.Companion.requiresFullStoragePermission(selectedDir.getAbsolutePath(), this)) {
                    checkAndRequestFullStoragePermission();
                }
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
