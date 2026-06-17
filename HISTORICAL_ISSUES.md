# Historical Issues

A record of bugs from the upstream MGit project that have been fixed in this fork.

| Issue | Title | Description |
|-------|-------|-------------|
| [#747](https://github.com/maks/MGit/issues/747) | Fatal Flaws on Branch Management | Creating a new branch on a repository with no commits caused the UI to display the new branch name even though the checkout failed, leading to commits silently being made on master. |
| [#749](https://github.com/maks/MGit/issues/749) | Deleted remote branches are still shown and cannot be removed | Remote branches that had been deleted on the server continued to appear in the branch list and could not be dismissed. |
| [#733](https://github.com/maks/MGit/issues/733) | Compatibility with latest Android version | Compatibility issues with recent Android releases. |
