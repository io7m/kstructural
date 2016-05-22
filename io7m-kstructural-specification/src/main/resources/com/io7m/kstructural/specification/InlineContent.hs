module InlineContent where

import qualified URI
import qualified ID

data InlineText = InlineText String

data InlineTerm = InlineTerm {
  termType    :: Maybe String,
  termContent :: [InlineText]
}

data Size = Size {
  sizeWidth  :: Integer,
  sizeHeight :: Integer
}

data InlineImage = InlineImage {
  imageType    :: Maybe String,
  imageTarget  :: URI.T,
  imageSize    :: Maybe Size,
  imageContent :: [InlineText]
}

data InlineVerbatim = InlineVerbatim {
  verbatimType    :: Maybe String,
  verbatimContent :: InlineText
}

data LinkContent
  = LCText  InlineText
  | LCImage InlineImage

data InlineLinkInternal = InlineLinkInternal {
  linkInternalTarget  :: ID.T,
  linkInternalContent :: [LinkContent]
}

data InlineLinkExternal = InlineLinkExternal {
  linkExternalTarget  :: URI.T,
  linkExternalContent :: [LinkContent]
}

data ListItem = ListItem {
  listItemContent :: [InlineContent]
}

data InlineListOrdered = InlineListOrdered {
  listOrderedItems :: [ListItem]
}

data InlineListUnordered = InlineListUnordered {
  listUnorderedItems :: [ListItem]
}

data InlineFootnoteRef = InlineFootnoteRef {
  footnoteTarget :: ID.T
}

type TableColumnName = [InlineText]

data TableHead = TableHead {
  tableHeadNames :: [TableColumnName]
}

data TableCell = TableCell {
  tableCellContent :: [InlineContent]
}

data TableRow = TableRow {
  tableRowCells :: [TableCell]
}

data TableBody = TableBody {
  tableBodyRows :: [TableRow]
}

data InlineTable = InlineTable {
  tableType    :: Maybe String,
  tableSummary :: [InlineText],
  tableHead    :: Maybe TableHead,
  tableBody    :: TableBody
}

tableCheck :: InlineTable -> Bool
tableCheck table =
  case (tableHead table) of
    Nothing -> True
    Just th ->
      let rows     = tableBodyRows $ tableBody table
          expected = length $ tableHeadNames th
      in all (\row -> length (tableRowCells row) == expected) rows

data InlineInclude = InlineInclude {
  includeFile :: String
}

data InlineContent
  = ICText          InlineText
  | ICTerm          InlineTerm
  | ICImage         InlineImage
  | ICVerbatim      InlineVerbatim
  | ICLink          InlineLinkInternal
  | ICLinkExternal  InlineLinkExternal
  | ICListOrdered   InlineListOrdered
  | ICListUnordered InlineListUnordered
  | ICFootnoteRef   InlineFootnoteRef
  | ICTable         InlineTable
  | ICInclude       InlineInclude

