# OpenBoard
<a href='https://f-droid.org/packages/org.dslul.openboard.inputmethod.latin'><img src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png' alt='Get it on F-Droid' height='80'></a>
<a href='https://play.google.com/store/apps/details?id=org.dslul.openboard.inputmethod.latin&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' height='80px'/></a>

![](images/feature.png)

100% FOSS keyboard, based on AOSP.

## Permissions
* **Read Contacts**: Used to add your contact list to suggestions. Disabled by default.

## Cannot open settings in MIUI
See https://github.com/dslul/openboard/issues/46

## Contribute

### How to create a dictionary
You can use [this tool](https://github.com/remi0s/aosp-dictionary-tools) to create a dictionary. You need a wordlist, as described [here](https://github.com/dslul/openboard/blob/master/dictionaries/sample.combined). The output .dict file must be put in [res/raw](https://github.com/dslul/openboard/tree/master/app/src/main/res/raw).

### APK Development

#### Linux

Install java:
```sh
sudo pacman -S jdk8-openjdk jre8-openjdk jre8-openjdk-headless
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

Install the platform tools for your target android version:
```sh
androidsdk "platform-tools" "platforms;android-28"
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
