#!/bin/sh

FILES=`ls *.sdi`
FILES="${FILES} `ls *.sd`"

cat <<EOF
[formal-item
  [title Documentation Listing]
  [list-unordered
EOF

for f in ${FILES}
do
  cat <<EOF
    [item [link-ext [target "$f"] "$f"]]
EOF
done

cat <<EOF
]]
EOF
