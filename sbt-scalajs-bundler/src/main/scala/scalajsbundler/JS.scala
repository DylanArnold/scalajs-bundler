package scalajsbundler

import java.util.concurrent.atomic.AtomicInteger

import org.scalajs.core.ir.Position
import org.scalajs.core.tools.javascript.Printers
import org.scalajs.core.tools.javascript.Trees._

object JS {

  implicit val position: Position = Position.NoPosition

  /** String literal */
  def str(value: String): StringLiteral =
    StringLiteral(value)

  /** Boolean literal */
  def bool(value: Boolean): BooleanLiteral = BooleanLiteral(value)

  /** Object literal */
  def obj(fields: (String, Tree)*): ObjectConstr =
    ObjectConstr(fields.map { case (ident, value) => (str(ident), value) }.to[List])

  def objStr(fields: Seq[(String, String)]): ObjectConstr =
    obj(fields.map { case (k, v) => k -> JS.str(v) }: _*)

  /** Array literal */
  def arr(elems: Tree*): ArrayConstr =
    ArrayConstr(elems.to[List])

  /** Variable reference */
  def ref(ident: String): VarRef =
    VarRef(Ident(ident))

  def regex(value: String): Tree =
    New(ref("RegExp"), List(str(value)))

  /** Block of several statements */
  def block(stats: Tree*): Tree = Block(stats.to[List])

  /** Anonymous function definition */
  def fun(body: VarRef => Tree): Function = {
    val param = freshIdentifier()
    Function(List(ParamDef(Ident(param), rest = false)), Return(body(ref(param))))
  }

  /** Name binding */
  def let(value: Tree)(usage: VarRef => Tree): Tree = {
    val ident = freshIdentifier()
    Block(VarDef(Ident(ident), value), usage(ref(ident)))
  }

  /** Name binding */
  def let(value1: Tree, value2: Tree)(usage: (VarRef, VarRef) => Tree): Tree = {
    val ident1 = freshIdentifier()
    val ident2 = freshIdentifier()
    Block(
      VarDef(Ident(ident1), value1),
      VarDef(Ident(ident2), value2),
      usage(ref(ident1), ref(ident2))
    )
  }

  def `new`(ctor: Tree, args: Tree*): New = New(ctor, args.to[List])

  def toJson(obj: ObjectConstr): String = show(obj, isStat = false)

  def show(tree: Tree, isStat: Boolean = true): String = {
    val writer = new java.io.StringWriter
    val printer = new Printers.JSTreePrinter(writer)
    printer.printTree(tree, isStat)
    writer.toString
  }

  object syntax {

    implicit class TreeSyntax(tree: Tree) {
      def `.` (ident: String): DotSelect = DotSelect(tree, Ident(ident))
      def bracket(ident: String): BracketSelect = BracketSelect(tree, str(ident))
      def bracket(ident: Tree): BracketSelect = BracketSelect(tree, ident)
      def := (rhs: Tree): Assign = Assign(tree, rhs)
      def apply(args: Tree*): Apply = Apply(tree, args.to[List])
    }

  }

  private val identifierSeq = new AtomicInteger(0)
  private def freshIdentifier(): String =
    s"x${identifierSeq.getAndIncrement()}"

}
