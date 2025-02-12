package io.github.arainko.ducktape

import io.github.arainko.ducktape.internal.NotQuotedException

import scala.annotation.{ compileTimeOnly, implicitNotFound }
import scala.deriving.Mirror
import scala.util.NotGiven

opaque type BuilderConfig[Source, Dest] = Unit

opaque type FallibleBuilderConfig[F[+x], Source, Dest] = Unit

object Field {

  @compileTimeOnly("'Field.const' needs to be erased from the AST with a macro.")
  def const[Source, Dest, FieldType, ActualType](selector: Dest => FieldType, value: ActualType)(using
    ev1: ActualType <:< FieldType,
    @implicitNotFound("Field.const is supported for product types only, but ${Source} is not a product type.")
    ev2: Mirror.ProductOf[Source],
    @implicitNotFound("Field.const is supported for product types only, but ${Dest} is not a product type.")
    ev3: Mirror.ProductOf[Dest]
  ): BuilderConfig[Source, Dest] = throw NotQuotedException("Field.const")

  @compileTimeOnly("'Field.computed' needs to be erased from the AST with a macro.")
  def computed[Source, Dest, FieldType, ActualType](selector: Dest => FieldType, f: Source => ActualType)(using
    ev1: ActualType <:< FieldType,
    @implicitNotFound("Field.computed is supported for product types only, but ${Source} is not a product type.")
    ev2: Mirror.ProductOf[Source],
    @implicitNotFound("Field.computed is supported for product types only, but ${Dest} is not a product type.")
    ev3: Mirror.ProductOf[Dest]
  ): BuilderConfig[Source, Dest] = throw NotQuotedException("Field.computed")

  @compileTimeOnly("'Field.renamed' needs to be erased from the AST with a macro.")
  def renamed[Source, Dest, SourceFieldType, DestFieldType](
    destSelector: Dest => DestFieldType,
    sourceSelector: Source => SourceFieldType
  )(using
    ev1: SourceFieldType <:< DestFieldType,
    @implicitNotFound("Field.renamed is supported for product types only, but ${Source} is not a product type.")
    ev2: Mirror.ProductOf[Source],
    @implicitNotFound("Field.renamed is supported for product types only, but ${Dest} is not a product type.")
    ev3: Mirror.ProductOf[Dest]
  ): BuilderConfig[Source, Dest] = throw NotQuotedException("Field.renamed")

  @compileTimeOnly("'Field.default' needs to be erased from the AST with a macro.")
  def default[Source, Dest, FieldType](selector: Dest => FieldType)(using
    @implicitNotFound("Field.default is supported for product types only, but ${Source} is not a product type.")
    ev1: Mirror.ProductOf[Source],
    @implicitNotFound("Field.default is supported for product types only, but ${Dest} is not a product type.")
    ev2: Mirror.ProductOf[Dest]
  ): BuilderConfig[Source, Dest] = throw new NotQuotedException("Field.default")

  @compileTimeOnly("'Field.allMatching' needs to be erased from the AST with a macro.")
  def allMatching[Source, Dest, FieldSource](
    fieldSource: FieldSource
  )(using
    @implicitNotFound("Field.allMatching is supported for product types only, but ${Source} is not a product type.")
    ev1: Mirror.ProductOf[Source],
    @implicitNotFound("Field.allMatching is supported for product types only, but ${Dest} is not a product type.")
    ev2: Mirror.ProductOf[Dest],
    @implicitNotFound("Field.allMatching is supported for product types only, but ${FieldSource} is not a product type.")
    ev3: Mirror.ProductOf[FieldSource]
  ): BuilderConfig[Source, Dest] = throw NotQuotedException("Field.allMatching")

  @compileTimeOnly("'Field.fallibleConst' needs to be erased from the AST with a macro.")
  def fallibleConst[F[+x], Source, Dest, FieldType, ActualType](selector: Dest => FieldType, value: F[ActualType])(using
    ev1: ActualType <:< FieldType,
    @implicitNotFound("Field.fallibleConst is supported for product types only, but ${Source} is not a product type.")
    ev2: Mirror.ProductOf[Source],
    @implicitNotFound("Field.fallibleConst is supported for product types only, but ${Dest} is not a product type.")
    ev3: Mirror.ProductOf[Dest]
  ): FallibleBuilderConfig[F, Source, Dest] = throw NotQuotedException("Field.fallibleConst")

  @compileTimeOnly("'Field.fallibleComputed' needs to be erased from the AST with a macro.")
  def fallibleComputed[F[+x], Source, Dest, FieldType, ActualType](selector: Dest => FieldType, f: Source => F[ActualType])(using
    ev1: ActualType <:< FieldType,
    @implicitNotFound("Field.fallibleComputed is supported for product types only, but ${Source} is not a product type.")
    ev2: Mirror.ProductOf[Source],
    @implicitNotFound("Field.fallibleComputed is supported for product types only, but ${Dest} is not a product type.")
    ev3: Mirror.ProductOf[Dest]
  ): FallibleBuilderConfig[F, Source, Dest] = throw NotQuotedException("Field.fallibleComputed")
}

object Case {

  @compileTimeOnly("'Case.const' needs to be erased from the AST with a macro.")
  def const[SourceSubtype]: Case.Const[SourceSubtype] = throw NotQuotedException("Case.const")

  opaque type Const[SourceSubtype] = Unit

  object Const {
    extension [SourceSubtype](inst: Const[SourceSubtype]) {

      @compileTimeOnly("'Case.const' needs to be erased from the AST with a macro.")
      def apply[Source, Dest](const: Dest)(using
        @implicitNotFound("Case.computed is only supported for coproducts but ${Source} is not a coproduct.")
        ev1: Mirror.SumOf[Source],
        ev2: SourceSubtype <:< Source,
        @implicitNotFound("Case.const is only supported for subtypes of ${Source}.")
        ev3: NotGiven[SourceSubtype =:= Source]
      ): BuilderConfig[Source, Dest] = throw NotQuotedException("Case.const")
    }
  }

  @compileTimeOnly("'Case.computed' needs to be erased from the AST with a macro.")
  def computed[SourceSubtype]: Case.Computed[SourceSubtype] = throw NotQuotedException("Case.computed")

  opaque type Computed[SourceSubtype] = Unit

  object Computed {
    extension [SourceSubtype](inst: Computed[SourceSubtype]) {

      @compileTimeOnly("'Case.computed' needs to be erased from the AST with a macro.")
      def apply[Source, Dest](f: SourceSubtype => Dest)(using
        @implicitNotFound("Case.computed is only supported for coproducts but ${Source} is not a coproduct.")
        ev1: Mirror.SumOf[Source],
        ev2: SourceSubtype <:< Source,
        @implicitNotFound("Case.computed is only supported for subtypes of ${Source}.")
        ev3: NotGiven[SourceSubtype =:= Source]
      ): BuilderConfig[Source, Dest] = throw NotQuotedException("Case.computed")
    }
  }

  @compileTimeOnly("'Case.fallibleConst' needs to be erased from the AST with a macro.")
  def fallibleConst[SourceSubtype]: Case.FallibleConst[SourceSubtype] = throw NotQuotedException("Case.fallibleComputed")

  opaque type FallibleConst[SourceSubtype] = Unit

  object FallibleConst {
    extension [SourceSubtype](inst: FallibleConst[SourceSubtype]) {

      @compileTimeOnly("'Case.fallibleConst' needs to be erased from the AST with a macro.")
      def apply[F[+x], Source, Dest](const: F[Dest])(using
        @implicitNotFound("Case.fallibleConst is only supported for coproducts but ${Source} is not a coproduct.")
        ev1: Mirror.SumOf[Source],
        ev2: SourceSubtype <:< Source,
        @implicitNotFound("Case.fallibleConst is only supported for subtypes of ${Source}.")
        ev3: NotGiven[SourceSubtype =:= Source]
      ): FallibleBuilderConfig[F, Source, Dest] = throw NotQuotedException("Case.computed")
    }
  }

  @compileTimeOnly("'Case.fallibleConst' needs to be erased from the AST with a macro.")
  def fallibleComputed[SourceSubtype]: Case.FallibleComputed[SourceSubtype] = throw NotQuotedException("Case.fallibleComputed")

  opaque type FallibleComputed[SourceSubtype] = Unit

  object FallibleComputed {
    extension [SourceSubtype](inst: FallibleComputed[SourceSubtype]) {

      @compileTimeOnly("'Case.fallibleComputed' needs to be erased from the AST with a macro.")
      def apply[F[+x], Source, Dest](const: SourceSubtype => F[Dest])(using
        @implicitNotFound("Case.fallibleComputed is only supported for coproducts but ${Source} is not a coproduct.")
        ev1: Mirror.SumOf[Source],
        ev2: SourceSubtype <:< Source,
        @implicitNotFound("Case.fallibleComputed is only supported for subtypes of ${Source}.")
        ev3: NotGiven[SourceSubtype =:= Source]
      ): FallibleBuilderConfig[F, Source, Dest] = throw NotQuotedException("Case.computed")
    }
  }
}
