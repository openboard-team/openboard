<h2 align="center"><b>OpenBoard</b></h2>
<h4 align="center">100% FOSS keyboard, based on AOSP.</h4>
<p align="center"><img src='fastlane/metadata/android/en-US/images/icon.png' height='128'></p>
<p align="center"><a href="https://github.com/openboard-team/openboard/actions/workflows/android-build.yml"><img src="https://img.shields.io/github/workflow/status/openboard-team/openboard/Build" alt="GitHub Workflow Status"></a>
<a href="https://github.com/openboard-team/openboard/releases"><img src="https://img.shields.io/github/v/release/openboard-team/openboard" alt="GitHub release (latest by date)"></a>
<a href="https://github.com/openboard-team/openboard/releases"><img src="https://img.shields.io/github/release-date/openboard-team/openboard" alt="GitHub Release Date"></a>
<a href="https://github.com/openboard-team/openboard/commits/master"><img src="https://img.shields.io/github/commits-since/openboard-team/openboard/latest" alt="GitHub commits since latest release (by date)"></a>
<a href="https://hosted.weblate.org/engage/openboard/"><img src="https://hosted.weblate.org/widgets/openboard/-/openboard/svg-badge.svg" alt="Translation status"></a>
<a href="https://matrix.to/#/#openboard:matrix.org?via=matrix.org"><img src="https://img.shields.io/matrix/openboard:matrix.org" alt="Matrix"></a></p>
<p align="center"><a href='https://f-droid.org/packages/org.dslul.openboard.inputmethod.latin'><img src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png' alt='Get it on F-Droid' height='60'></a>
<a href='https://play.google.com/store/apps/details?id=org.dslul.openboard.inputmethod.latin&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' height='60'/></a></p>  

## Community chat
Join our [matrix] channel [here](https://matrix.to/#/#openboard:matrix.org?via=matrix.org).

<img src="images/matrix_qr.png" alt="Matrix QR Code" height="128">

## Contribute

### Translate OpenBoard
You can help in translating OpenBoard in your language through our [Weblate project](https://hosted.weblate.org/engage/openboard/).
[![Translation status](https://hosted.weblate.org/widgets/openboard/-/openboard/horizontal-blue.svg)](https://hosted.weblate.org/engage/openboard/)

### Create a dictionary
You can use [this tool](https://github.com/remi0s/aosp-dictionary-tools) to create a dictionary. You need a wordlist, as described [here](dictionaries/sample.combined). The output .dict file must be put in [res/raw](app/src/main/res/raw), its wordlist in [dictionnaries](/dictionaries).

For your dictionnary to be merged, you need to provide the wordlist you used, as well as its license if any.

### APK Development

#### Linux

Install java:
```sh
sudo pacman -S jdk11-openjdk jre11-openjdk jre11-openjdk-headless
```

Install Android SDK:
```sh
sudo pacman -S snapd
sudo snap install androidsdk
```

Configure your SDK location in your `~/.bash_profile` or `~/.bashrc`:
```bash
export ANDROID_SDK_ROOT=~/snap/androidsdk/current/AndroidSDK/
```

Compile the project. This will install all dependencies, make sure to accept
licenses when prompted.

```sh
./gradlew assembleDebug
```

Connect your phone and install the debug APK
```sh
adb install ./app/build/outputs/apk/debug/app-debug.apk
```

#### Generate KeyboardTextsTable.java
Make your modifications in [tools/make-keyboard-text/src/main/resources](tools/make-keyboard-text/src/main/resources)/values-YOUR LOCALE.

Generate the new version of [KeyboardTextsTable.java](app/src/main/java/org/dslul/openboard/inputmethod/keyboard/internal/KeyboardTextsTable.java):
```sh
./gradlew tools:make-keyboard-text:makeText
```

## Credits
- Icon by [Marco TLS](https://www.marcotls.eu)
- [AOSP Keyboard](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/)
- [LineageOS](https://review.lineageos.org/admin/repos/LineageOS/android_packages_inputmethods_LatinIME)
- [Simple Keyboard](https://github.com/rkkr/simple-keyboard)
- [Indic Keyboard](https://gitlab.com/indicproject/indic-keyboard)
- Our [contributors](https://github.com/openboard-team/openboard/graphs/contributors)
