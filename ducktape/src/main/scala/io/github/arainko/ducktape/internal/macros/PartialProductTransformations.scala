package io.github.arainko.ducktape.internal.macros

import scala.quoted.*
import io.github.arainko.ducktape.PartialTransformer
import io.github.arainko.ducktape.internal.modules.*
import scala.deriving.Mirror
import scala.annotation.tailrec
import scala.util.chaining.*

object PartialProductTransformations {
  def transformFailFast[F[+x]: Type, Source: Type, Dest: Type](
    Source: Expr[Mirror.ProductOf[Source]],
    Dest: Expr[Mirror.ProductOf[Dest]],
    support: Expr[PartialTransformer.FailFast.Support[F]],
    sourceValue: Expr[Source]
  )(using Quotes): Expr[F[Dest]] = {
    import quotes.reflect.*

    given Fields.Source = Fields.Source.fromMirror(Source)
    given Fields.Dest = Fields.Dest.fromMirror(Dest)

    failFastFieldTransformations[F, Source, Dest](support, sourceValue, Fields.dest.value)
  }

  def transformAcc[F[+x]: Type, Source: Type, Dest: Type](
    Source: Expr[Mirror.ProductOf[Source]],
    Dest: Expr[Mirror.ProductOf[Dest]],
    support: Expr[PartialTransformer.Accumulating.Support[F]],
    sourceValue: Expr[Source]
  )(using Quotes): Expr[F[Dest]] = {
    import quotes.reflect.*

    given Fields.Source = Fields.Source.fromMirror(Source)
    given Fields.Dest = Fields.Dest.fromMirror(Dest)

    accumulatingFieldTransformations[F, Source, Dest](support, sourceValue, Fields.dest.value)
      .get
      .tap(exp => println(exp.asTerm.show(using Printer.TreeShortCode)))
  }

  inline def transform[F[+x], Source, Dest](
    sourceValue: Source
  )(using support: PartialTransformer.FailFast.Support[F], Source: Mirror.ProductOf[Source], Dest: Mirror.ProductOf[Dest]) =
    ${ transformFailFast[F, Source, Dest]('Source, 'Dest, 'support, 'sourceValue) }

  inline def transformAccumulating[F[+x], Source, Dest](
    sourceValue: Source
  )(using support: PartialTransformer.Accumulating.Support[F], Source: Mirror.ProductOf[Source], Dest: Mirror.ProductOf[Dest]) =
    ${ transformAcc[F, Source, Dest]('Source, 'Dest, 'support, 'sourceValue) }

  private def failFastFieldTransformations[F[+x]: Type, Source: Type, Dest: Type](
    support: Expr[PartialTransformer.FailFast.Support[F]],
    sourceValue: Expr[Source],
    fieldsToTransformInto: List[Field]
  )(using Quotes, Fields.Source) = {
    import quotes.reflect.*

    val transformedFields =
      fieldsToTransformInto.map { dest =>
        val source =
          Fields.source
            .get(dest.name)
            .getOrElse(Failure.abort(Failure.NoFieldMapping(dest.name, summon[Type[Source]])))

        source.partialTransformerTo[F, PartialTransformer.FailFast](dest).asExpr match {
          case '{ $transformer: PartialTransformer.FailFast[F, src, dest] } =>
            val sourceField = sourceValue.accessField(source).asExprOf[src]
            Field.Wrapped(dest, '{ $transformer.transform($sourceField) })
        }
      }

    nestFlatMaps[F, Dest](support, transformedFields)
  }

  private def accumulatingFieldTransformations[F[+x]: Type, Source: Type, Dest: Type](
    support: Expr[PartialTransformer.Accumulating.Support[F]],
    sourceValue: Expr[Source],
    fieldsToTransformInto: List[Field]
  )(using Quotes, Fields.Source) = {
    import quotes.reflect.*

    val transformedFields =
      fieldsToTransformInto.map { dest =>
        val source =
          Fields.source
            .get(dest.name)
            .getOrElse(Failure.abort(Failure.NoFieldMapping(dest.name, summon[Type[Source]])))

        source.partialTransformerTo[F, PartialTransformer.Accumulating](dest).asExpr match {
          case '{ $transformer: PartialTransformer.Accumulating[F, src, dest] } =>
            val sourceField = sourceValue.accessField(source).asExprOf[src]
            Field.Wrapped(dest, '{ $transformer.transform($sourceField) })
        }
      }

    zipFields[F, Dest](support, transformedFields)
      .map(zipped =>
        zipped match {
          case '{ $zipped: F[a] } =>
            '{ $support.map($zipped, value => ${ 
                unnestPairs[Dest](fieldsToTransformInto, 'value).get
            }) 
            }
        }
      )
  }

  private def nestFlatMaps[F[+x]: Type, Dest: Type](
    support: Expr[PartialTransformer.FailFast.Support[F]],
    wrappedFields: List[Field.Wrapped[F]]
  )(using Quotes): Expr[F[Dest]] = {
    def recurse(
      leftoverWrappedFields: List[Field.Wrapped[F]],
      collectedUnwrappedFields: List[Field.Unwrapped]
    )(using Quotes): Expr[F[Dest]] =
      leftoverWrappedFields match {
        case Field.Wrapped(name, value) :: Nil =>
          value match {
            case '{ $value: F[destField] } =>
              '{
                $support
                  .map[`destField`, Dest]($value, a => ${ construct(Field.Unwrapped(name, 'a) :: collectedUnwrappedFields) })
              }
          }

        case Field.Wrapped(name, value) :: next =>
          value match {
            case '{ $value: F[destField] } =>
              '{
                $support.flatMap[`destField`, Dest](
                  $value,
                  a => ${ recurse(next, Field.Unwrapped(name, 'a) :: collectedUnwrappedFields) }
                )
              }
          }

        case Nil =>
          val constructedValue = construct(collectedUnwrappedFields)
          '{ $support.pure[Dest]($constructedValue) }
      }

    recurse(wrappedFields, Nil)
  }

  private def zipFields[F[+x]: Type, Dest: Type](
    support: Expr[PartialTransformer.Accumulating.Support[F]],
    wrappedFields: List[Field.Wrapped[F]]
  )(using Quotes): Option[Expr[F[Any]]] =
    wrappedFields.map(_.value).reduceLeftOption { (accumulated, current) =>
      (accumulated -> current) match {
        case '{ $accumulated: F[a] } -> '{ $current: F[b] } =>
          '{ $support.product[`a`, `b`]($accumulated, $current) }
      }
    }

  private def construct[Dest: Type](fieldValues: List[Field.Unwrapped])(using Quotes) = {
    import quotes.reflect.*
    val namedArgs = fieldValues.map(field => NamedArg(field.underlying.name, field.value.asTerm))
    Constructor(TypeRepr.of[Dest]).appliedToArgs(namedArgs).asExprOf[Dest]
  }

  private def unnestPairs[Dest: Type](fields: List[Field], nestedPairs: Expr[Any])(using Quotes) = {
    import quotes.reflect.*

    ZippedProduct
      .unzip(nestedPairs, fields)
      .map { (pattern, unwrappedFields) =>
        Match(nestedPairs.asTerm, List(CaseDef(pattern, None, construct[Dest](unwrappedFields).asTerm))).asExprOf[Dest]
      }
  }
}
