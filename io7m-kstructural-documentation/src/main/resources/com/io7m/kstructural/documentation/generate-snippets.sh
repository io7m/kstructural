#!/bin/sh -ex

JAR_FILE=../../../../../../../../io7m-kstructural-cmdline/target/io7m-kstructural-cmdline-0.1.0-main.jar

java -jar "${JAR_FILE}" 2>&1 | sed -n -e '/^    check/,/^$/ p' | tee gen/cmdline-usage-check.txt
java -jar "${JAR_FILE}" 2>&1 | sed -n -e '/^    compile-xhtml/,/^$/ p' | tee gen/cmdline-usage-compile-xhtml.txt
java -jar "${JAR_FILE}" 2>&1 | sed -n -e '/^    convert/,/^$/ p' | tee gen/cmdline-usage-convert.txt

FILES=`ls *.sdi`
FILES="${FILES} `ls *.sd`"

cat <<EOF
[list-unordered
EOF

for f in ${FILES}
do
  cat <<EOF
  [item [link-ext [target "$f"] "$f"]]
EOF
done

cat <<EOF
]
EOF
