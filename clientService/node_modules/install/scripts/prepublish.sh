#!/usr/bin/env bash

cd $(dirname $0)/..

terser install.js -c -m > install.min.js
