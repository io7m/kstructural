module SEMatcher where

import qualified SExpression as SE

data Rule
  = MAnySymbol
  | MAnyQuoted
  | MAnyList
  | MExactSymbol String
  | MExactQuoted String
  | MExactList [Rule]
  | MChoice [Rule]
  | MListVariadic [Rule] Rule
  deriving Eq

instance Show Rule where
  show (MExactSymbol s)    = "(#exact-symbol " ++ s ++ ")"
  show (MExactQuoted s)    = "(#exact-quoted " ++ s ++ ")"
  show (MExactList s)      = "(#exact-list [" ++ (concatMap show s) ++ "])"
  show  MAnySymbol         = "#any-symbol"
  show  MAnyQuoted         = "#any-quoted"
  show  MAnyList           = "#any-list"
  show (MChoice s)         = "(#choice [" ++ (concatMap show s) ++ "])"
  show (MListVariadic s m) = "(#variadic [" ++ (concatMap show s) ++ "]" ++ (show m) ++ ")"

matches :: SE.Expr -> Rule -> Bool
matches (SE.ESymbol e) (MExactSymbol s)    = e == s
matches (SE.ESymbol _) (MExactQuoted _)    = False
matches (SE.ESymbol _) (MExactList _)      = False
matches (SE.ESymbol _) MAnySymbol          = True
matches (SE.ESymbol _) MAnyQuoted          = False
matches (SE.ESymbol _) MAnyList            = False
matches (SE.ESymbol _) (MListVariadic _ _) = False
matches (SE.EQuoted _) MAnySymbol          = False
matches (SE.EQuoted _) MAnyQuoted          = True
matches (SE.EQuoted _) MAnyList            = False
matches (SE.EQuoted e) (MExactQuoted s)    = e == s
matches (SE.EQuoted _) (MExactSymbol _)    = False
matches (SE.EQuoted _) (MExactList _)      = False
matches (SE.EQuoted _) (MListVariadic _ _) = False
matches (SE.EList _)   (MExactSymbol _) = False
matches (SE.EList _)   (MExactQuoted _) = False
matches (SE.EList _)   MAnySymbol       = False
matches (SE.EList _)   MAnyQuoted       = False
matches (SE.EList _)   MAnyList         = True
matches (SE.EList s) (MExactList m) =
  (length s) == (length m) && all (\(xe,xm) -> matches xe xm) (zip s m)
matches (SE.EList s) (MListVariadic m0 mr) =
  if (length s >= length m0)
  then
    let
      prefix_zip = take (length m0) (zip s m0)
      prefix_ok  = all (\(xe,xm) -> matches xe xm) prefix_zip
      suffix     = drop (length m0) s
      suffix_zip = zip suffix (replicate (length suffix) mr)
      suffix_ok  = all (\(xe,xm) -> matches xe xm) suffix_zip
    in
      prefix_ok && suffix_ok
  else
    False
matches e (MChoice m0)= 1 == length (filter (matches e) m0)
