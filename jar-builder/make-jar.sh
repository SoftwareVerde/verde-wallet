#!/bin/bash

rm -f *.hprof &>/dev/null

script_root="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [[ -e /usr/libexec/java_home ]]; then
    JAVA_HOME="$(/usr/libexec/java_home -v1.8)"
fi

app_java_dir=$(cd ${script_root}'/../app/src/main/java' && pwd)

tmp_java_dir="${script_root}/java/src/main/java"
tmp_android_dir="${script_root}/android/src/main/java"

rm -rf $script_root/java/src/
rm -rf $script_root/android/src/
mkdir -p "${tmp_java_dir}"
mkdir -p "${tmp_android_dir}"

function copy_java_files() {
    copy_local_files "${app_java_dir}" "$1" "$2" "$tmp_java_dir"
}

function copy_android_files() {
    copy_local_files "${app_java_dir}" "$1" "$2" "$tmp_android_dir"
}

function copy_local_files() {
    java_src_dir="$1"
    package="$2"
    files="$3"
    destination="$4"

    echo "Local copy - $package ($files)"

    mkdir -p ${destination}/${package}

    if [[ "${files}" == "*" ]]; then
        cp ${java_src_dir}/${package}/*.java "${destination}/${package}/."
    elif [[ "${files}" == "**" ]]; then
        cp -R ${java_src_dir}/${package}/* "${destination}/${package}/."
    else
        for file in ${files}; do
            cp ${java_src_dir}/${package}/${file}.java "${destination}/${package}/."
        done
    fi
}

# standard java files
copy_java_files "com/softwareverde/bitcoin/app/lib" "*"

# android files
copy_android_files "com/softwareverde/bitcoin/app/database" "**"
copy_android_files "com/softwareverde/database/android/sqlite" "**"
cat << ANDROID_MANIFEST > $tmp_android_dir/../AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest
    xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.softwareverde.bitcoin.app.android" />
ANDROID_MANIFEST

dublin_identity_android_path="../../dublin-identity-android"

# create local.properties if possible
if [[ ! -e local.properties ]]; then
    if [[ -e $dublin_identity_android_path/local.properties ]]; then
        ln -s $dublin_identity_android_path/local.properties local.properties
    elif [[ -e ../local.properties ]]; then
        ln -s ../local.properties local.properties
    fi
fi

./gradlew --warning-mode all clean build -x lint || exit 1

