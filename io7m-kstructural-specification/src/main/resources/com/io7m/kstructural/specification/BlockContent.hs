module BlockContent where

import qualified ID
import qualified InlineContent as I

data BlockParagraph = BlockParagraph {
  para_type    :: Maybe String,
  para_id      :: Maybe ID.T,
  para_content :: [I.InlineContent]
}

data BlockFormalItem = BlockFormalItem {
  formal_type    :: Maybe String,
  formal_id      :: Maybe ID.T,
  formal_title   :: [I.InlineText],
  formal_content :: [I.InlineContent]
}

data BlockFootnote = BlockFootnote {
  footnote_type    :: Maybe String,
  footnote_id      :: ID.T,
  footnote_content :: [I.InlineContent]
}

data SubsectionContent
  = SCParagraph  BlockParagraph
  | SCFormalItem BlockFormalItem
  | SCFootnote   BlockFootnote

data BlockSubsection = BlockSubsection {
  subsection_type    :: Maybe String,
  subsection_id      :: Maybe ID.T,
  subsection_title   :: [I.InlineText],
  subsection_content :: [SubsectionContent]
}

data BlockSection = BlockSection {
  section_type    :: Maybe String,
  section_id      :: Maybe ID.T,
  section_title   :: [I.InlineText],
  section_content :: Either [BlockSubsection] [SubsectionContent]
}

data BlockPart = BlockPart {
  part_type    :: Maybe String,
  part_id      :: Maybe ID.T,
  part_title   :: [I.InlineText],
  part_content :: [BlockSection]
}

data BlockDocument = BlockDocument {
  document_type    :: Maybe String,
  document_id      :: Maybe ID.T,
  document_title   :: [I.InlineText],
  document_content :: Either [BlockPart] [BlockSection]
}

data BlockImport = BlockImport {
  importFile :: String
}

data BlockContent
  = BCParagraph  BlockParagraph
  | BCFormalItem BlockFormalItem
  | BCFootnote   BlockFootnote
  | BCSubsection BlockSubsection
  | BCSection    BlockSection
  | BCPart       BlockPart
  | BCDocument   BlockDocument
  | BCImport     BlockImport
