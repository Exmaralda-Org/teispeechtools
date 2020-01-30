#!/usr/bin/env bash
URL=${1:-http://github.com/Exmaralda-Org/teispeechtools}
wget "${URL}" -O -|egrep -o -e 'id=\S+' | sed -e 's_id="_id="'"${URL}"'#_'
