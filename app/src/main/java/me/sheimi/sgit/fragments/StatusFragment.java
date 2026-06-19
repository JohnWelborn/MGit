package me.sheimi.sgit.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.manichord.mgit.repodetail.RepoDetailViewModel;

import me.sheimi.android.activities.SheimiFragmentActivity.OnBackClickListener;
import me.sheimi.sgit.R;
import me.sheimi.sgit.activities.CommitDiffActivity;
import me.sheimi.sgit.database.models.Repo;

/**
 * Created by sheimi on 8/5/13.
 */
public class StatusFragment extends RepoDetailFragment {

    private Repo mRepo;
    private ProgressBar mLoadding;
    private TextView mStatus;
    private Button mUnstagedDiff;
    private Button mStagedDiff;

    public static StatusFragment newInstance(Repo mRepo) {
        StatusFragment fragment = new StatusFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Repo.TAG, mRepo);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_status, container, false);
        getRawActivity().setStatusFragment(this);

        Bundle bundle = getArguments();
        mRepo = (Repo) bundle.getSerializable(Repo.TAG);
        if (mRepo == null && savedInstanceState != null) {
            mRepo = (Repo) savedInstanceState.getSerializable(Repo.TAG);
        }
        if (mRepo == null) {
            return v;
        }
        mLoadding = (ProgressBar) v.findViewById(R.id.loading);
        mStatus = (TextView) v.findViewById(R.id.status);
        mStagedDiff = (Button) v.findViewById(R.id.button_staged_diff);
        mUnstagedDiff = (Button) v.findViewById(R.id.button_unstaged_diff);
        mStagedDiff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDiff("HEAD", "dircache");
            }
        });
        mUnstagedDiff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDiff("dircache", "filetree");
            }
        });

        RepoDetailViewModel vm = new ViewModelProvider(requireActivity()).get(RepoDetailViewModel.class);
        vm.getStatusText().observe(getViewLifecycleOwner(), status -> {
            if (mStatus != null && mLoadding != null) {
                mStatus.setText(status);
                mLoadding.setVisibility(View.GONE);
                mStatus.setVisibility(View.VISIBLE);
            }
        });

        reset();
        return v;
    }

    private void showDiff(String oldCommit, String newCommit) {
        Intent intent = new Intent(getRawActivity(),
                CommitDiffActivity.class);
        intent.putExtra(CommitDiffActivity.OLD_COMMIT, oldCommit);
        intent.putExtra(CommitDiffActivity.NEW_COMMIT, newCommit);
        intent.putExtra(CommitDiffActivity.SHOW_DESCRIPTION, false);
        intent.putExtra(Repo.TAG, mRepo);
        getRawActivity().startActivity(intent);
    }

    @Override
    public void reset() {
        if (mLoadding == null || mStatus == null)
            return;
        mLoadding.setVisibility(View.VISIBLE);
        mStatus.setVisibility(View.GONE);
        new ViewModelProvider(requireActivity()).get(RepoDetailViewModel.class).loadStatus(mRepo);
    }

    @Override
    public OnBackClickListener getOnBackClickListener() {
        return null;
    }
}
