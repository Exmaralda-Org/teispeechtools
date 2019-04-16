#!/usr/bin/env bash
declare -i i
let i=0
FILE=${1%.txt}
./spindel.sh text2iso --input=$1 --output=${FILE}-0-text2iso.xml --indent
step=text2iso
inc_i(){
    LAST_STEP=${step}
    last_i=${i}
    let i+=1
}
inc_i
for step in segmentize guess normalize pos identify unidentify align
do
    ./spindel.sh ${step} --input=${FILE}-${last_i}-${LAST_STEP}.xml \
        --indent --output=${FILE}-${i}-${step}.xml --time 43
    inc_i
done
