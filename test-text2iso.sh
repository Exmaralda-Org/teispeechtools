#!/bin/sh
rm -f TEST_LOG.txt
VERSION=0.6.3
JAR=target/teispeechtools-$VERSION.jar
for f in src/test/txt/Input-*.txt ; do
    file=${f##*/}
    file=${file%%.txt}
    java -cp 'target/dependency/*' -jar $JAR -i "$f" -o "$file"-indent.xml --indent text2iso || echo ${f} failed | tee -a TEST_LOG.txt
    java -cp 'target/dependency/*' -jar $JAR -i "$f" -o "$file".xml text2iso
done

java -cp 'target/dependency/*' -jar $JAR -i "Input-correct-from-Spec-indent.xml" -o "Input-correct-from-Spec-indent-segmented".xml segmentize
