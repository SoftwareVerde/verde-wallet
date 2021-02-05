#!/bin/bash

./gradlew verdewalletlib:assembleRelease

echo -n "Library File: "
ls verdewalletlib/build/outputs/aar/*.aar

