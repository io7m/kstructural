module ImperativeContent where

import qualified InlineContent as I
import qualified ID

data ImperativeParagraph = ImperativeParagraph {
  para_type    :: Maybe String,
  para_id      :: Maybe ID.T
} deriving Eq

data ImperativeFormalItem = ImperativeFormalItem {
  formal_type  :: Maybe String,
  formal_id    :: Maybe ID.T,
  formal_title :: [I.InlineText]
} deriving Eq

data ImperativeFootnote = ImperativeFootnote {
  footnote_type :: Maybe String,
  footnote_id   :: ID.T
} deriving Eq

data ImperativeSubsection = ImperativeSubsection {
  subsection_type  :: Maybe String,
  subsection_id    :: Maybe ID.T,
  subsection_title :: [I.InlineText]
} deriving Eq

data ImperativeSection = ImperativeSection {
  section_type  :: Maybe String,
  section_id    :: Maybe ID.T,
  section_title :: [I.InlineText]
} deriving Eq

data ImperativePart = ImperativePart {
  part_type  :: Maybe String,
  part_id    :: Maybe ID.T,
  part_title :: [I.InlineText]
} deriving Eq

data ImperativeDocument = ImperativeDocument {
  document_type  :: Maybe String,
  document_id    :: Maybe ID.T,
  document_title :: [I.InlineText]
} deriving Eq

data ImperativeImport = ImperativeImport {
  importFile :: String
} deriving Eq

data ImperativeContent
  = ICParagraph  ImperativeParagraph
  | ICFormalItem ImperativeFormalItem
  | ICFootnote   ImperativeFootnote
  | ICSubsection ImperativeSubsection
  | ICSection    ImperativeSection
  | ICPart       ImperativePart
  | ICDocument   ImperativeDocument
  | ICImport     ImperativeImport
  deriving Eq

importContent :: ImperativeImport -> ImperativeContent
importContent _ = undefined

instance Ord ImperativeContent where
  compare (ICParagraph _) (ICParagraph _)   = EQ
  compare (ICParagraph _) (ICFormalItem _)  = EQ
  compare (ICParagraph _) (ICFootnote _)    = EQ
  compare (ICParagraph _) (ICSubsection _)  = LT
  compare (ICParagraph _) (ICSection _)     = LT
  compare (ICParagraph _) (ICPart _)        = LT
  compare (ICParagraph _) (ICDocument _)    = LT
  compare (ICFormalItem _) (ICParagraph _)  = EQ
  compare (ICFormalItem _) (ICFormalItem _) = EQ
  compare (ICFormalItem _) (ICFootnote _)   = EQ
  compare (ICFormalItem _) (ICSubsection _) = LT
  compare (ICFormalItem _) (ICSection _)    = LT
  compare (ICFormalItem _) (ICPart _)       = LT
  compare (ICFormalItem _) (ICDocument _)   = LT
  compare (ICFootnote _) (ICParagraph _)  = EQ
  compare (ICFootnote _) (ICFormalItem _) = EQ
  compare (ICFootnote _) (ICFootnote _)   = EQ
  compare (ICFootnote _) (ICSubsection _) = LT
  compare (ICFootnote _) (ICSection _)    = LT
  compare (ICFootnote _) (ICPart _)       = LT
  compare (ICFootnote _) (ICDocument _)   = LT
  compare (ICSubsection _) (ICParagraph _)  = GT
  compare (ICSubsection _) (ICFormalItem _) = GT
  compare (ICSubsection _) (ICFootnote _)   = GT
  compare (ICSubsection _) (ICSubsection _) = EQ
  compare (ICSubsection _) (ICSection _)    = LT
  compare (ICSubsection _) (ICPart _)       = LT
  compare (ICSubsection _) (ICDocument _)   = LT
  compare (ICSection _) (ICParagraph _)  = GT
  compare (ICSection _) (ICFormalItem _) = GT
  compare (ICSection _) (ICFootnote _)   = GT
  compare (ICSection _) (ICSubsection _) = GT
  compare (ICSection _) (ICSection _)    = EQ
  compare (ICSection _) (ICPart _)       = LT
  compare (ICSection _) (ICDocument _)   = LT
  compare (ICPart _) (ICParagraph _)  = GT
  compare (ICPart _) (ICFormalItem _) = GT
  compare (ICPart _) (ICFootnote _)   = GT
  compare (ICPart _) (ICSubsection _) = GT
  compare (ICPart _) (ICSection _)    = GT
  compare (ICPart _) (ICPart _)       = EQ
  compare (ICPart _) (ICDocument _)   = LT
  compare (ICDocument _) (ICParagraph _)  = GT
  compare (ICDocument _) (ICFormalItem _) = GT
  compare (ICDocument _) (ICFootnote _)   = GT
  compare (ICDocument _) (ICSubsection _) = GT
  compare (ICDocument _) (ICSection _)    = GT
  compare (ICDocument _) (ICPart _)       = GT
  compare (ICDocument _) (ICDocument _)   = EQ
  compare (ICImport i) e = compare (importContent i) e
  compare e (ICImport i) = compare e (importContent i)