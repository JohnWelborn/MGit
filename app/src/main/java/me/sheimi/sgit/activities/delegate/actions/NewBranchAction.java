package me.sheimi.sgit.activities.delegate.actions;

import me.sheimi.android.activities.SheimiFragmentActivity;
import me.sheimi.sgit.R;
import me.sheimi.sgit.activities.RepoDetailActivity;
import me.sheimi.sgit.database.models.Repo;

/**
 * Created by liscju - piotr.listkiewicz@gmail.com on 2015-03-15.
 */
public class NewBranchAction extends RepoAction {
    public NewBranchAction(Repo mRepo, RepoDetailActivity mActivity) {
        super(mRepo, mActivity);
    }

    @Override
    public void execute() {
        mActivity.showEditTextDialog(R.string.dialog_create_branch_title,
                R.string.dialog_create_branch_hint, R.string.label_create,
                new SheimiFragmentActivity.OnEditTextDialogClicked() {
                    @Override
                    public void onClicked(String branchName) {
                        mActivity.getRepoDetailViewModel().checkout(mRepo, null, branchName, branchName);
                    }
                });
        mActivity.closeOperationDrawer();
    }
}
