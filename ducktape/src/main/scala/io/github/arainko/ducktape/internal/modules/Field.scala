package io.github.arainko.ducktape.internal.modules

import io.github.arainko.ducktape.Transformer

import scala.quoted.*

private[ducktape] final class Field(val name: String, val tpe: Type[?]) {
  def transformerTo(that: Field)(using Quotes): Expr[Transformer[?, ?]] = {
    import quotes.reflect.*

    (tpe -> that.tpe) match {
      case '[src] -> '[dest] =>
        Implicits.search(TypeRepr.of[Transformer[src, dest]]) match {
          case success: ImplicitSearchSuccess => success.tree.asExprOf[Transformer[src, dest]]
          case err: ImplicitSearchFailure     => Failure.abort(Failure.TransformerNotFound(this, that, err.explanation))
        }
    }
  }

  // This untyped due to not being able to reduce a HKT with wildcards
  def partialTransformerTo[
    F[+x]: Type,
    PartialTransformer[f[+x], a, b] <: Transformer.FailFast[f, a, b] | Transformer.Accumulating[f, a, b]: Type
  ](that: Field)(using Quotes): quotes.reflect.Term = {
    import quotes.reflect.*

    (tpe -> that.tpe) match {
      case '[src] -> '[dest] =>
        Implicits.search(TypeRepr.of[PartialTransformer[F, src, dest]]) match {
          case success: ImplicitSearchSuccess => success.tree
          case err: DivergingImplicit         => report.errorAndAbort(err.explanation)
          case err: NoMatchingImplicits       => report.errorAndAbort(err.explanation)
          case err: AmbiguousImplicits        => report.errorAndAbort(err.explanation)
          case err: ImplicitSearchFailure     =>
            // probably hitting another compiler bug here, derivation works for
            // FailFast[Option, TypeA, TypeB] but bugs out on things like:
            // FailFast[[A] =>> Either[::[String], A], TypeA, TypeB]
            // the line below fixes it...
            '{ compiletime.summonInline[PartialTransformer[F, src, dest]] }.asTerm
        }
    }
  }

  def <:<(that: Field)(using Quotes): Boolean = {
    import quotes.reflect.*
    TypeRepr.of(using tpe) <:< TypeRepr.of(using that.tpe)
  }

}

private[ducktape] object Field {
  final case class Unwrapped(underlying: Field, value: Expr[Any])

  final case class Wrapped[F[+x]](underlying: Field, value: Expr[F[Any]])
}
