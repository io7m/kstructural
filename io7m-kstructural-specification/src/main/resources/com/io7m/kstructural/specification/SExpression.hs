module SExpression where

data Expr
  = ESymbol String
  | EQuoted String
  | EList [Expr]
  deriving (Eq, Show)

