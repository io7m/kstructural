module IDExistence where

import qualified ID
import qualified BlockContent as BC
import qualified Control.Monad as CM

class HasID a where
  hasID :: ID.T -> a -> Maybe ID.T

maybeEq :: Eq a => a -> Maybe a -> Maybe a
maybeEq x Nothing  = Nothing
maybeEq x (Just y) = if x == y then Just y else Nothing

instance HasID BC.BlockParagraph where
  hasID k b = maybeEq k (BC.para_id b)

instance HasID BC.BlockFormalItem where
  hasID k b = maybeEq k (BC.formal_id b)

instance HasID BC.BlockFootnote where
  hasID k b = maybeEq k (Just $ BC.footnote_id b)

instance HasID BC.SubsectionContent where
  hasID k (BC.SCParagraph b)  = hasID k b
  hasID k (BC.SCFormalItem b) = hasID k b
  hasID k (BC.SCFootnote b)   = hasID k b

instance HasID BC.BlockSubsection where
  hasID k b =
    case maybeEq k (BC.subsection_id b) of
      Just x  -> Just k
      Nothing -> CM.foldM hasID k (BC.subsection_content b)

instance HasID BC.BlockSection where
  hasID k b =
    case maybeEq k (BC.section_id b) of
      Just x  -> Just k
      Nothing ->
        case BC.section_content b of
          Left ss  -> CM.foldM hasID k ss
          Right sc -> CM.foldM hasID k sc

instance HasID BC.BlockPart where
  hasID k b =
    case maybeEq k (BC.part_id b) of
      Just x  -> Just k
      Nothing -> CM.foldM hasID k (BC.part_content b)

instance HasID BC.BlockDocument where
  hasID k b =
    case maybeEq k (BC.document_id b) of
      Just x  -> Just k
      Nothing ->
        case BC.document_content b of
          Left p  -> CM.foldM hasID k p
          Right s -> CM.foldM hasID k s
