#!/bin/sh -ex

SOURCE=InlineContent.hs

mkdir -p gen
grep -e '^data InlineText' ${SOURCE} > gen/InlineText.txt
sed -n -e '/data InlineContent/,/^$/ p' ${SOURCE} > gen/InlineContent.txt
sed -n -e '/data InlineFootnoteRef/,/^$/ p' ${SOURCE} > gen/InlineFootnoteRef.txt
sed -n -e '/data InlineImage/,/}/ p' ${SOURCE} > gen/InlineImage.txt
sed -n -e '/data InlineLinkExternal/,/}/ p' ${SOURCE} > gen/InlineLinkExternal.txt
sed -n -e '/data InlineLinkInternal/,/}/ p' ${SOURCE} > gen/InlineLinkInternal.txt
sed -n -e '/data InlineListOrdered/,/}/ p' ${SOURCE} > gen/InlineListOrdered.txt
sed -n -e '/data InlineListUnordered/,/}/ p' ${SOURCE} > gen/InlineListUnordered.txt
sed -n -e '/data InlineTable/,/^$/ p' ${SOURCE} > gen/InlineTable.txt
sed -n -e '/data InlineTerm/,/}/ p' ${SOURCE} > gen/InlineTerm.txt
sed -n -e '/data InlineInclude/,/}/ p' ${SOURCE} > gen/InlineInclude.txt
sed -n -e '/data InlineVerbatim/,/}/ p' ${SOURCE} > gen/InlineVerbatim.txt
sed -n -e '/data LinkContent/,/^$/ p' ${SOURCE} > gen/LinkContent.txt
sed -n -e '/data ListItem/,/}/ p' ${SOURCE} > gen/ListItem.txt
sed -n -e '/data Size/,/}/ p' ${SOURCE} > gen/Size.txt
sed -n -e '/data TableBody/,/}/ p' ${SOURCE} > gen/TableBody.txt
sed -n -e '/data TableCell/,/}/ p' ${SOURCE} > gen/TableCell.txt
sed -n -e '/data TableColumnName/,/}/ p' ${SOURCE} > gen/TableColumnName.txt
sed -n -e '/data TableHead/,/}/ p' ${SOURCE} > gen/TableHead.txt
sed -n -e '/data TableRow/,/}/ p' ${SOURCE} > gen/TableRow.txt
sed -n -e '/tableCheck/,/^$/ p' ${SOURCE} > gen/TableCheck.txt

SOURCE=BlockContent.hs

sed -n -e '/data BlockParagraph/,/}/ p' ${SOURCE} > gen/BlockParagraph.txt
sed -n -e '/data BlockFormalItem/,/}/ p' ${SOURCE} > gen/BlockFormalItem.txt
sed -n -e '/data BlockFootnote/,/}/ p' ${SOURCE} > gen/BlockFootnote.txt
sed -n -e '/data SubsectionContent/,/^$/ p' ${SOURCE} > gen/SubsectionContent.txt
sed -n -e '/data BlockSubsection/,/}/ p' ${SOURCE} > gen/BlockSubsection.txt
sed -n -e '/data BlockSection/,/}/ p' ${SOURCE} > gen/BlockSection.txt
sed -n -e '/data BlockPart/,/}/ p' ${SOURCE} > gen/BlockPart.txt
sed -n -e '/data BlockDocument/,/}/ p' ${SOURCE} > gen/BlockDocument.txt
sed -n -e '/data BlockImport/,/}/ p' ${SOURCE} > gen/BlockImport.txt

SOURCE=SEMatcher.hs

sed -n -e '/data Rule/,/^$/ p' ${SOURCE} > gen/Rule.txt
sed -n -e '/instance Show Rule/,/^$/ p' ${SOURCE} > gen/RuleShow.txt
sed -n -e '/^matches ::/,/^$/ p' ${SOURCE} > gen/RuleMatches.txt

