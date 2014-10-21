#!/bin/sh

apktool=$(which apktool)
version=$($apktool --version)
original_apk=$1

function detect_version()
{
if [ "$version" \> "1.5.2" ]
then
    echo "-o"       # new output folder syntax for version 2.0.0-RC2
else
    echo ""
fi
}

output_parameter=$(detect_version)

# prepare the directory structure
rm -rf patch
mkdir -p patch

# disassemble original apk and injected apk
$apktool d bin/injector.apk $output_parameter patch/injector
$apktool d $original_apk $output_parameter patch/orig

# injecting introspection classes
cp -r patch/injector/smali/* patch/orig/smali/

# insert the service
sed -i -e 's/<\/application>/<service android:name="com.sysdream.fino.InspectionService" android:enabled="true" android:exported="true"><intent-filter><action android:name="com.sysdream.fino.inspection"\/><\/intent-filter><\/service><\/application>/' patch/orig/AndroidManifest.xml

# fix sdk versions
sed -i -e 's/<uses-sdk[^>]+>/<uses-sdk android:minSdkVersion="14" android:targetSdkVersion="15" \/>/' patch/orig/AndroidManifest.xml

# build the unsigned apk
apktool b patch/orig $output_parameter tmp.apk

# sign the final apk
jarsigner -verbose -sigalg MD5withRSA -digestalg SHA1 -keystore ~/.android/debug.keystore -storepass android -signedjar $2 tmp.apk androiddebugkey
