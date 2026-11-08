#!/bin/sh
# Starts Yarn Calculator. Needs a Java JDK (version 17 or newer).
cd "$(dirname "$0")" || exit 1
exec java YarnCalculator.java "$@"
