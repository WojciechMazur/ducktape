package io.github.arainko.ducktape

import scala.quoted.*

trait Transformer2[Source, Dest] {
  def transform(value: Source): Dest
}

object Transformer2 {
  // def derivedTransformer[A: Type, B: Type](using Quotes): Expr[Transformer2[A, B]] =
  //   '{ src => ${ Interpreter.createTransformation[A, B]('src) }  }

  // inline given derived[Source, Dest]: Transformer2[Source, Dest] = ${ derivedTransformer[Source, Dest] }

  trait Custom[Source, Dest] extends Transformer2[Source, Dest]
}
