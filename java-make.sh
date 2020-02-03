#!/bin/bash

#set -e
#set -o xtrace

if [ ! -d bin/j2objc ]; then
    ./download-j2objc.sh
fi

script_root="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

make=${script_root}'/bin/j2objc/j2objc'
app_java_dir=$(cd ${script_root}'/app/src/main/java' && pwd)

tmp_dir="${script_root}/tmp"
tmp_java_dir="${tmp_dir}/java"
tmp_objc_dir="${tmp_dir}/objc"
tmp_resources_dir="${tmp_dir}/resources"
jar_cache_dir="${script_root}/.j2objc-cache"

rm -rf "${tmp_dir}" 2>/dev/null

mkdir -p "${jar_cache_dir}"
mkdir -p "${tmp_dir}"
mkdir -p "${tmp_java_dir}"
mkdir -p "${tmp_objc_dir}"
mkdir -p "${tmp_resources_dir}"

function copy_java_files() {
    copy_local_files "${app_java_dir}" "$1" "$2"
}

function copy_local_files() {
    java_src_dir="$1"
    package="$2"
    files="$3"

    echo "Local copy - $package ($files)"

    mkdir -p ${tmp_java_dir}/${package}

    if [[ "${files}" == "*" ]]; then
        cp ${java_src_dir}/${package}/*.java "${tmp_java_dir}/${package}/."
    elif [[ "${files}" == "**" ]]; then
        cp -R ${java_src_dir}/${package}/* "${tmp_java_dir}/${package}/."
    else
        for file in ${files}; do
            cp ${java_src_dir}/${package}/${file}.java "${tmp_java_dir}/${package}/."
        done
    fi
}

function copy_softwareverde_dependency() {
    package="$1"
    project="$2"
    version="$3"

    echo "GitHub download - $package:$project:$version"

    dependency_dir="${tmp_dir}/${package}"
    mkdir -p "${dependency_dir}"
    cd "${dependency_dir}"
    git clone --quiet https://github.com/softwareverde/${project}.git
    cd "${project}"
    git checkout --quiet "${version}" 2>/dev/null
    cd ${script_root}
    cp -R ${dependency_dir}/${project}/src/main/java/* "${tmp_java_dir}/."
}

function copy_maven_dependency() {
    # https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-slf4j-impl/2.11.0/
    package="$1"
    name="$2"
    version="$3"

    echo "Maven download - $package:$name:$version"

    jar_file="${name}-${version}-sources.jar"
    download_url="https://repo1.maven.org/maven2/$(echo "${package}" | sed 's/\./\//g')/${name}/${version}/${jar_file}"

    old_dir=`pwd`
    mkdir -p tmp/jar
    cd tmp/jar/
    if [ ! -f "${jar_cache_dir}/${jar_file}" ]; then
        cd "${jar_cache_dir}"
        wget --quiet "${download_url}"
        cd -
    fi
    mkdir -p unzip
    mv "${jar_cache_dir}/${jar_file}" unzip/.
    cd unzip
    unzip -q "${jar_file}"
    mv "${jar_file}" "${jar_cache_dir}/."
    # rm -r org/bouncycastle/gpg/test 2>/dev/null # Remove test files...
    find . -type d -iname 'test' | xargs rm -rf 2>/dev/null # Remove test files...
    # rm -rf org/bouncycastle/x509 2>/dev/null
    if [[ -d "javax" ]]; then
        cp -R javax ${tmp_java_dir}'/.'
    fi
    if [[ -d "com" ]]; then
        cp -R com ${tmp_java_dir}'/.'
    fi
    if [[ -d "org" ]]; then
        cp -R org ${tmp_java_dir}'/.'
    fi
    if [[ -d "net" ]]; then
        cp -R net ${tmp_java_dir}'/.'
    fi
    if [[ -d "sun" ]]; then
        cp -R sun ${tmp_java_dir}'/.'
    fi
    cd ..
    rm -rf unzip
    cd "${old_dir}"
}

copy_maven_dependency 'org.bouncycastle' 'bcprov-jdk15on' '1.60'
copy_maven_dependency 'org.bouncycastle' 'bcpg-jdk15on' '1.60'

copy_maven_dependency 'org.slf4j' 'slf4j-api' '1.7.28'
copy_maven_dependency 'org.slf4j' 'slf4j-simple' '1.7.28'

copy_maven_dependency 'org.apache.commons' 'commons-lang3' '3.7'

copy_softwareverde_dependency "com.softwareverde.util" "java-util" "v2.0.8"
copy_softwareverde_dependency "com.softwareverde.logging" "java-logging" "v2.1.0"
copy_softwareverde_dependency "com.softwareverde.json" "json" "v1.0.5"
copy_softwareverde_dependency "com.softwareverde.async" "java-async" "v0.3.0"
copy_softwareverde_dependency "com.softwareverde.db" "java-db" "v3.0.3"
copy_softwareverde_dependency "com.softwareverde.db-mysql" "java-db-mysql" "v4.0.4"
copy_softwareverde_dependency "com.softwareverde.http-client" "java-http-client" "v2.0.1"
copy_softwareverde_dependency "com.softwareverde.bitcoin-verde" "bitcoin-verde" "f7bc6521"

# com.softwareverde.bitcoin

echo "Removing problem files..."
rm -f $tmp_java_dir/org/bouncycastle/x509/util/LDAPStoreHelper.java \
    $tmp_java_dir/org/bouncycastle/jce/provider/X509LDAPCertStoreSpi.java \
    $tmp_java_dir/org/bouncycastle/jce/provider/X509StoreLDAPCertPairs.java \
    $tmp_java_dir/org/bouncycastle/jce/provider/X509StoreLDAPAttrCerts.java \
    $tmp_java_dir/org/bouncycastle/jce/provider/X509StoreLDAPCRLs.java \
    $tmp_java_dir/org/bouncycastle/jce/provider/X509StoreLDAPCerts.java \
    $tmp_java_dir/org/bouncycastle/pqc/crypto/qtesla/QTeslaKeyEncodingTests.java \
    $tmp_java_dir/org/bouncycastle/jcajce/provider/asymmetric/edec/KeyPairGeneratorSpi.java \
    2>/dev/null
rm -f $tmp_java_dir/com/softwareverde/util/SystemUtil.java
rm -f $tmp_java_dir/com/softwareverde/util/jni/NativeUtil.java
rm -r $tmp_java_dir/com/softwareverde/database/mysql/MysqlDatabaseConnectionFactory.java \
    $tmp_java_dir/com/softwareverde/database/mysql/MysqlDatabase.java

echo "Overwriting stubs..."
cp -r stubs/* $tmp_java_dir/

# temporary fix to avoid issue with bitcoin-verde having old java-logging version and doing weird things
nodeModulePath="$tmp_java_dir/com/softwareverde/bitcoin/server/module/node/NodeModule.java"
sed -i "" 's/Logger.LOG/null/' $nodeModulePath

echo "Running j2objc..."
cd "${tmp_objc_dir}"
java_make_args="-Xlint:none"
find ../java -iname '*.java' | xargs "${make}" $java_make_args -sourcepath "../java/" || exit 1

cd "${script_root}"

echo "Installing files..."
rm -fr "${script_root}/ios/java"
mkdir -p "${script_root}/ios/java"
cp -R ${tmp_objc_dir}/. "${script_root}/ios/java/."

echo -e "\nSUCCESS\n"

