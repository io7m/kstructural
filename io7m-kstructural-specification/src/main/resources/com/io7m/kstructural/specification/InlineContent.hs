module InlineContent where

import qualified URI
import qualified ID

data InlineText =
  InlineText String
  deriving Eq

data InlineTerm = InlineTerm {
  termType    :: Maybe String,
  termContent :: [InlineText]
} deriving Eq

data Size = Size {
  sizeWidth  :: Integer,
  sizeHeight :: Integer
} deriving Eq

data InlineImage = InlineImage {
  imageType    :: Maybe String,
  imageTarget  :: URI.T,
  imageSize    :: Maybe Size,
  imageContent :: [InlineText]
} deriving Eq

data InlineVerbatim = InlineVerbatim {
  verbatimType    :: Maybe String,
  verbatimContent :: InlineText
} deriving Eq

data LinkContent
  = LCText  InlineText
  | LCImage InlineImage
  deriving Eq

data InlineLinkInternal = InlineLinkInternal {
  linkInternalTarget  :: ID.T,
  linkInternalContent :: [LinkContent]
} deriving Eq

data InlineLinkExternal = InlineLinkExternal {
  linkExternalTarget  :: URI.T,
  linkExternalContent :: [LinkContent]
} deriving Eq

data ListItem = ListItem {
  listItemContent :: [InlineContent]
} deriving Eq

data InlineListOrdered = InlineListOrdered {
  listOrderedItems :: [ListItem]
} deriving Eq

data InlineListUnordered = InlineListUnordered {
  listUnorderedItems :: [ListItem]
} deriving Eq

data InlineFootnoteRef = InlineFootnoteRef {
  footnoteTarget :: ID.T
} deriving Eq

type TableColumnName = [InlineText]

data TableHead = TableHead {
  tableHeadNames :: [TableColumnName]
} deriving Eq

data TableCell = TableCell {
  tableCellContent :: [TableCellContent]
} deriving Eq

data TableCellContent
  = TCText          InlineText
  | TCTerm          InlineTerm
  | TCImage         InlineImage
  | TCVerbatim      InlineVerbatim
  | TCLink          InlineLinkInternal
  | TCLinkExternal  InlineLinkExternal
  | TCListOrdered   InlineListOrdered
  | TCListUnordered InlineListUnordered
  | TCFootnoteRef   InlineFootnoteRef
  | TCInclude       InlineInclude
  deriving Eq

data TableRow = TableRow {
  tableRowCells :: [TableCell]
} deriving Eq

data TableBody = TableBody {
  tableBodyRows :: [TableRow]
} deriving Eq

data InlineTable = InlineTable {
  tableType    :: Maybe String,
  tableSummary :: [InlineText],
  tableHead    :: Maybe TableHead,
  tableBody    :: TableBody
} deriving Eq

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
} deriving Eq

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
  deriving Eq
