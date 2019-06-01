import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser

sealed class Expression

data class Point(val x : Int, val y : Int)
data class Rect(val tl : Point, val br : Point)

data class Mask(val es : List<Expression>)
data class Spacing(val num : Int) : Expression()
data class ViaCost(val num : Int) : Expression()
data class Boundary(val boundary : Rect) : Expression()
data class MetalLayers(val num : Int) : Expression()

object CircuitGrammar : Grammar<Mask>() {
    val numT by token("\\d+")
    val eq by token("=")
    val eol by token("\n")
    val wsT by token(" +")
    val ws by optional(wsT)

    val lpar by token("\\(")
    val rpar by token("\\)")
    val comma by token(",")

    val num by numT use { text.toInt() }
    val point by -lpar * -ws * num * -ws * -comma * -ws * num * -ws * -rpar
    val pointExpr by point use { Point(t1, t2) }

    val rect by pointExpr * -ws * pointExpr * -ws
    val rectExpr by rect use { Rect(t1, t2) }

    val spacingT by token("Spacing")
    val viaCostT by token("ViaCost")
    val boundaryT by token("Boundary")
    val metalLayersT by token("#MetalLayers")
    val routedViasT by token("#RoutedVias")
    val obstaclesT by token("#Obstacles")
    val routedShapeT by token("RoutedShape")



    val definition by -ws * -eq * -ws * num * -ws * -eol
    val spacing by -spacingT * -ws * definition
    val viaCost by -viaCostT * -ws * definition
    val boundary by -boundaryT * -ws * -eq * -ws * rectExpr * -eol
    val metalLayers by -metalLayersT * definition

    val spacingExpr by spacing use { Spacing(this) }
    val viaCostExpr by viaCost use { ViaCost(this) }
    val boundaryExpr by boundary use { Boundary(this) }
    val metalLayersExpr by metalLayers use { MetalLayers(this)}


    val expression by viaCostExpr or spacingExpr or boundaryExpr or metalLayersExpr

    val mask by oneOrMore(expression) use { Mask(this) }


    override val rootParser: Parser<Mask> by mask
}

fun main(args: Array<String>) {
    val expr = "ViaCost=10\n"+
            "Spacing = 8\n" +
            "Boundary = (0,0) (7000,3000)\n" +
            "#MetalLayers = 3\n"
//    val expr = "ViaCost = 10\n" +
//            "Spacing = 8\n" +
//            "Boundary = (0,0) (7000,3000)\n" +
//            "#MetalLayers = 3\n" +
//            "#RoutedShapes = 10\n" +
//            "#RoutedVias = 3\n" +
//            "#Obstacles = 10\n" +
//            "RoutedShape M2 (694,482) (700,517)\n" +
//            "RoutedShape M3 (743,1704) (790,1746)\n" +
//            "RoutedShape M3 (876,2731) (927,2751)\n" +
//            "RoutedShape M3 (4768,2513) (4774,2538)\n" +
//            "RoutedShape M1 (3288,1903) (3292,1907)\n" +
//            "RoutedShape M1 (4522,2640) (4618,2689)\n" +
//            "RoutedShape M3 (3155,1724) (3200,1740)\n" +
//            "RoutedShape M1 (8,523) (109,660)\n" +
//            "RoutedShape M2 (5501,1497) (5638,1530)\n" +
//            "RoutedShape M3 (2484,977) (2594,987)\n" +
//            "RoutedVia V1 (447,1411)\n" +
//            "RoutedVia V1 (4861,2421)\n" +
//            "RoutedVia V1 (648,1955)\n" +
//            "Obstacle M3 (6985,0) (7000,168)\n" +
//            "Obstacle M1 (5134,4) (5233,134)\n" +
//            "Obstacle M3 (5760,2813) (6035,2934)\n" +
//            "Obstacle M3 (5195,49) (5308,89)\n" +
//            "Obstacle M2 (5165,0) (5430,57)\n" +
//            "Obstacle M2 (2473,1529) (2733,1632)\n" +
//            "Obstacle M3 (1660,2152) (1917,2213)\n" +
//            "Obstacle M3 (943,2446) (1187,2505)\n" +
//            "Obstacle M1 (769,1568) (781,1650)\n" +
//            "Obstacle M1 (5157,1196) (5412,1295)"
    println("testing: $expr")
    val result = CircuitGrammar.parseToEnd(expr)
    println(result)
}