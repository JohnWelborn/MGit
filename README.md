# MGit

MGit is a Git client Android App.

This is a continuation of [the SGit project](https://github.com/sheimi/SGit)
and a fork of the unmaintained [maks/MGit](https://github.com/maks/MGit).

## Changes in this fork

* All Files Access permission is now optional, which may enable Play Store distribution (discussed in [maks/MGit#733](https://github.com/maks/MGit/issues/733))
* **Pull All** button on the main toolbar — fetch/pull every repo at once
* **Paste Key** option in the private key manager
* Fetch prunes stale remote-tracking branches automatically (fixes [maks/MGit#749](https://github.com/maks/MGit/issues/749))
* Compatible with Android 14+ (fixes [maks/MGit#733](https://github.com/maks/MGit/issues/733))
* Build modernized for JDK 21, AGP 8.x, and SDK 34, with GitHub Actions replacing Travis CI
* Background operations (clone, fetch, pull, push, commit, etc.) migrated from deprecated AsyncTask to Kotlin Coroutines (addresses [maks/MGit#277](https://github.com/maks/MGit/issues/277))

## Notes

* If you encounter any issues (bugs, crashes, etc.) and want to help improve this project, please open an issue on [GitHub](https://github.com/maks/MGit/issues/new) describing: what the issues are; and how they were caused, to allow for re-creation and fixing of bugs.
* This app requires minimum of for Android v5.0
* **Editing Files:** MGit does not include a text editor. To edit files, install an external editor app that supports File Providers, such as [Viper Edit](https://github.com/maks/viper-edit).

## Supported Features

* Create local repositories
* Clone remote repositories
* Pull from origin
* Delete local repositories
* Browse files
* Browse commit messages (short)
* Checkout branches and tags
* HTTP/HTTPS/SSH are supported (including SSH with private key passphrase)
* Username/Password authentication is supported
* Search local repositories
* Private key management
* Manually choose code language
* `git diff` between commits
* Import existing repositories (that is, you can copy a repository from computer and import to MGit)
* Checkout remote branches
* Merge branches
* Push merged content
* Edit file via external app that can edit the given file type
* Commit and push changed files
* Committer information
* Prompt for password
* *Option* to save username/password
* `git status`
* Cancel when cloning
* Add modified file to stage
* View state of staged files (aka index)
* `git rebase`
* `git cherrypick`
* `git checkout <file>` (reset changes of a file)

## Quick start

### Clone a remote repository

1. Click on the `+` icon to add a new repository
2. Enter remote URL (see URL format below)
3. Enter local repository name - note that this is **not** the full path, as MGit stores all  
repositories in the same local directory (can be changed in MGit settings)
4. Click the `Clone` button
5. If required, you will be prompted for credentials to connect to the remote repo. MGit will download the repository (all branches) to your device

### Create a local repository
1. Click on the `+` icon to add a new repository
2. Click on `Init Local` to create a local repository
3. Enter the name for this repository when prompted
4. A local empty repo will be created

### URL format

#### SSH URLs

* SSH running on standard port (22): `ssh://username@server_name/path/to/repo`
* SSH running on non-standard port: `ssh://username@server_name:port/path/to/repo`
* `username` is needed - by default, MGit tries to connect as root.

#### HTTP(S) URLs

* HTTP(S) URL: `https://server_name/path/to/repo`

## License

See [GPLv3](./LICENSE)

All code written by `maks@manichord.com` can at your option also be used under the [MIT license](https://en.wikipedia.org/wiki/MIT_License).

## Help

If you want to help improve this project, contributions, especially translations are very welcome. Also contributions to documentation via the wiki for this repo are also most welcome!

### Contributing code

If you would like to contribute code, either a bugfix or a new feature, please make sure there is a open issue that addresses the new code. 
**No Pull Requests** will be merged that do not reference an existing issue in the repo.

Please use the Android Studio formatting settings set for this project in the repo.

All strings visible to the user need to go into strings resource file. 

#### Project Goals

* Provide the best GUI git client available on any platform
* Be usable on both phone, tablet and laptop form-factor devices

#### Non-goals for the project

* Support for proprietary vendor APIs (eg. Github)
