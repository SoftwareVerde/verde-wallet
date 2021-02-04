#!/bin/bash

script_root="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "${script_root}"

mkdir -p bin
cd bin

version='2.5'
url="https://github.com/google/j2objc/releases/download/${version}/j2objc-${version}.zip"
#version="master"
#url="https://github.com/google/j2objc/archive/master.zip"

if [ -d "j2objc" ]; then
    echo "j2objc exists. Aborting."
    exit 1
fi

curl -O -L "${url}"

unzip "j2objc-${version}.zip"
rm "j2objc-${version}.zip"

mv j2objc-${version} j2objc


# only when compiling
#unzip master.zip
#rm master.zip

#mv j2objc-${version} j2objc_src

#cd j2objc
#make dist
#mv dist ../j2objc

