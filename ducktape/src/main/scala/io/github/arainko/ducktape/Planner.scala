package io.github.arainko.ducktape

import scala.quoted.*
import io.github.arainko.ducktape.internal.modules.*

object Planner {
  import Structure.*

  inline def print[A, B] = ${ printMacro[A, B] }

  def printMacro[A: Type, B: Type](using Quotes): Expr[Unit] = {
    val plan = createPlan[A, B]
    quotes.reflect.report.info(plan.toString())
    '{}
  }

  def createPlan[Source: Type, Dest: Type](using Quotes): Plan[Plan.Error] = recurseAndCreatePlan[Source, Dest](Plan.Context(Type[Source], Type[Dest], Vector.empty))

  private def recurseAndCreatePlan[Source: Type, Dest: Type](context: Plan.Context)(using Quotes): Plan[Plan.Error] = {
    val src = Structure.of[Source]
    val dest = Structure.of[Dest]
    val plan = recurse(src, dest, context)
    val updated = plan.updateAt(Type.of[Sum1.Leaf2] :: "duspko" :: Nil)(plan => Plan.BetweenSingletons(Type.of[Int], Type.of[Int], '{ 123 }))
    println(updated.get.show)
    println()
    println(plan.show)
    plan
    // plan
  }

  private def recurse(source: Structure, dest: Structure, context: Plan.Context)(using Quotes): Plan[Plan.Error] =
    (source.force -> dest.force) match {
      case UserDefinedTransformation(transformer) =>
        Plan.UserDefined(source.tpe, dest.tpe, transformer)

      case (source, dest) if source.typeRepr <:< dest.typeRepr =>
        Plan.Upcast(source.tpe, dest.tpe)

      case Structure('[Option[srcTpe]], srcName) -> Structure('[Option[destTpe]], destName) =>
        Plan.BetweenOptions(Type[srcTpe], Type[destTpe], recurseAndCreatePlan[srcTpe, destTpe](context))

      case Structure('[a], _) -> Structure('[Option[destTpe]], destName) =>
        Plan.BetweenNonOptionOption(Type[a], Type[destTpe], recurseAndCreatePlan[a, destTpe](context))

      case Structure(source @ '[Iterable[srcTpe]], srcName) -> Structure(dest @ '[Iterable[destTpe]], destName) =>
        Plan.BetweenCollections(dest, Type[srcTpe], Type[destTpe], recurseAndCreatePlan[srcTpe, destTpe](context))

      case (source: Product, dest: Product) =>
        val fieldPlans = dest.fields.map { (destField, destFieldStruct) =>
          val plan = 
            source.fields
              .get(destField)
              .map(recurse(_, destFieldStruct, context.add(destField)))
              .getOrElse(Plan.Error(source.tpe, dest.tpe, context.add(destField), s"No field named '$destField' found in ${source.tpe.repr.show}"))
          destField -> plan
        }
        Plan.BetweenProducts(source.tpe, dest.tpe, fieldPlans)

      case (source: Coproduct, dest: Coproduct) =>
        val casePlans = source.children.map { (sourceName, sourceCaseStruct) =>
          val plan = 
            dest
              .children
              .get(sourceName)
              .map(recurse(sourceCaseStruct, _, context.add(sourceCaseStruct.tpe)))
              .getOrElse(Plan.Error(source.tpe, dest.tpe, context.add(sourceCaseStruct.tpe), s"No child named '$sourceName' found in ${dest.tpe.repr.show}"))
          sourceName -> plan
        }
        Plan.BetweenCoproducts(source.tpe, dest.tpe, casePlans)

      case (source: Structure.Singleton, dest: Structure.Singleton) if source.name == dest.name =>
        Plan.BetweenSingletons(source.tpe, dest.tpe, dest.value)

      case (source, dest) => 
        Plan.Error(source.tpe, dest.tpe, context, s"Couldn't build a transformation plan between ${source.tpe.repr.show} and ${dest.tpe.repr.show}")
    }

  object UserDefinedTransformation {
    def unapply(structs: (Structure, Structure))(using Quotes): Option[Expr[UserDefinedTransformer[?, ?]]] = {
      val (src, dest) = structs

      (src.tpe -> dest.tpe) match {
        case '[src] -> '[dest] => Expr.summon[UserDefinedTransformer[src, dest]]
      }
    }
  }
}
