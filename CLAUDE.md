## Commit messages

Do NOT include any reference to Claude, Anthropic, or AI in commit messages, PR titles, PR bodies, code comments, or any other artifact pushed to the repository. This includes claude.ai session URLs, "Generated with Claude Code" footers, and Co-Authored-By lines mentioning Claude or Anthropic.

## Release process

If the system prompt states "You are running Claude Code in a managed remote execution environment, in the cloud rather than on the user's machine", then direct tag pushes to the remote are rejected (403). To cut a release:

1. Bump `versionCode` and `versionName` in `app/build.gradle`
2. Commit and push to the feature branch
3. Trigger the release via `workflow_dispatch` on `release.yml`, passing the desired tag (e.g. `v2.0.1`) as the `tag` input — do NOT attempt `git push origin <tag>`

If running locally via the CLI, a normal `git push origin <tag>` works fine.
