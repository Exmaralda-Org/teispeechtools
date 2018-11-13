rm -f TEST_LOG.txt
for f in src/test/txt/Input-*.txt ; do
    file=${f##*/}
    file=${file%%.txt}
    java -cp 'target/dependency/*' -jar target/teispeechtools-0.2-SNAPSHOT.jar -i "$f" -o "$file"-indent.xml --indent text2iso || echo ${f} failed | tee -a TEST_LOG.txt
    java -cp 'target/dependency/*' -jar target/teispeechtools-0.2-SNAPSHOT.jar -i "$f" -o "$file".xml text2iso
done
