package io.github.arainko.ducktape.internal.modules

import scala.quoted.*

private[ducktape] object Constructor {
  def apply(using Quotes)(tpe: quotes.reflect.TypeRepr): quotes.reflect.Term = {
    import quotes.reflect.*

    val (repr, constructor, tpeArgs) =
      tpe match {
        case AppliedType(repr, reprArguments) => (repr, repr.typeSymbol.primaryConstructor, reprArguments)
        case notApplied                       => (tpe, tpe.typeSymbol.primaryConstructor, Nil)
      }

    New(Inferred(repr))
      .select(constructor)
      .appliedToTypes(tpeArgs)
  }
}
