import java.lang.Exception

class ParseError(i : Int, text : String, error : String)
    : Exception(error + ":" + Parse(i, text))

class Parse(val i : Int, val text : String) {
    fun fail(message :String) : ParseError {
        return ParseError(i, text, message)
    }

    override fun toString() : String {
        val a = text.substring(0, i)
        val b = text.substring(i)
        return "Parse{$i, \"$a\", \"$b\"}"
    }
}

sealed class Either<A, B>
data class Left<A, B>(val left: A) : Either<A, B>()
data class Right<A, B>(val right: B) : Either<A, B>()

abstract class Grammar<T> {
    abstract fun check(input : Parse) : Pair<T, Parse>
}

class Map<T1, T2>(val grammar: Grammar<T1>, val f :(p: T1) -> T2 ) : Grammar<T2>()
{
    override fun check(input : Parse) : Pair<T2, Parse> {
        val (a, b) = grammar.check(input)
        return Pair(f(a), b)
    }
}

class Or<T1, T2>(val g1: Grammar<T1>, val g2: Grammar<T2>) : Grammar<Either<T1, T2>>()
{
    override fun check(input: Parse) : Pair<Either<T1, T2>, Parse>{

        try {
            val (a1, b1) = g1.check(input)
            return Pair(Left(a1), b1)
        }
        catch (e : ParseError) {
            try {
                val (a2, b2) = g2.check(input)
                return Pair(Right(a2), b2)
            }
            catch(e2 : ParseError) {
                throw input.fail(e.message + " <+or+> " + e2.message)
            }
        }
    }
}
//
//fun concat(x : Any, y : Any) : Any
//{
//    when(x) {
//        is List<*> -> {
//            when(y) {
//                is List<*> -> {
//                    return x + y
//                }
//            }
//            return x + listOf(y)
//        }
//    }
//    when(y) {
//        is List<*> ->{
//            return listOf(x) + y
//        }
//    }
//    return listOf(x, y)
//}
//
//fun flatten(x : Any ) : Any {
//    when(x) {
//        is Pair<*,*> -> {
//            val (a, b) = x
//            val la = if (a != null) flatten(a) else ArrayList<Any>()
//            val lb = if (b != null) flatten(b) else ArrayList<Any>()
//            return concat(la, lb)
//        }
//    }
//    return x
//}


class And<T1, T2>(val g1: Grammar<T1>, val g2: Grammar<T2>) : Grammar<Pair<T1, T2>>()
{
    override fun check(input: Parse) : Pair<Pair<T1, T2>, Parse>{
        val (a1, b1) = g1.check(input)
        val (a2, b2) = g2.check(b1)
        return Pair(Pair(a1, a2), b2)
    }
}

class Skip<T>(val g: Grammar<T>) : Grammar<Unit>() {
    override fun check(input: Parse): Pair<Unit, Parse> {
        val (_, p) = g.check(input)
        return Pair(Unit, p)
    }
}

class Sequence(val gs : List<Grammar<out Any>>) : Grammar<List<Any>>()
{
    override fun check(input: Parse): Pair<List<Any>, Parse> {
        var p = input
        val l = ArrayList<Any>()
        gs.forEach {
            val (a, p2) = it.check(p)
            if (a != Unit) l.add(a)
            p = p2
        }
        return Pair(l.toList(), p)
    }
}

class Many<T>(val g : Grammar<T>) : Grammar<List<T>>() {
    override fun check(input: Parse): Pair<List<T>, Parse> {
        var p = input
        val l = ArrayList<T>()
        while (true) {
            try {
                val (a, p2) = g.check(p)
                l.add(a)
                p = p2
            }
            catch (e : ParseError) {
                break
            }
        }
        return Pair(l.toList(), p)
    }
}

class Optional<T>(val g : Grammar<T>) : Grammar<T?>() {
    override fun check(input: Parse): Pair<T?, Parse> {
        try {
            return g.check(input)
        }
        catch (e : ParseError) {
            return Pair(null, input)
        }
    }
}

class Many1<T>(val g : Grammar<T>) : Grammar<List<T>>() {
    override fun check(input: Parse): Pair<List<T>, Parse> {
        val (a1, p1) = g.check(input)
        var p = p1
        val l = ArrayList<T>()
        l.add(a1)
        while (true) {
            try {
                val (a, p2) = g.check(p)
                l.add(a)
                p = p2
            }
            catch (e : ParseError) {
                break
            }
        }
        return Pair(l.toList(), p)
    }
}

operator fun<T1, T2> Grammar<T1>.plus(b : Grammar<T2>): Grammar<Either<T1, T2>> {
    return Or<T1, T2>(this, b)
}

operator fun<T1, T2> Grammar<T1>.times(b : Grammar<T2>): Grammar<Pair<T1, T2>> {
    return And<T1, T2>(this, b)
}


class Token(val text : String) : Grammar<String>() {
    val expr = text.toRegex()

    override fun check(input : Parse) : Pair<String, Parse> {
        val res = expr.find(input.text, input.i)
        if(res != null) {
            return Pair(res.value, Parse(input.i+res.value.length, input.text))
        }
        throw input.fail("Could not match $text")
    }
}

fun<T> parseToEnd(text : String, g : Grammar<T>) : T {
    val x = Parse(0, text)
    val (r, p) = g.check(x)
    if(p.i != p.text.length) throw p.fail("Parse Incomplete")
    return r
}


fun main() {

    val ws = Token("\\s*")
    val eq = Token("=\\s*")
    val eol = Token("\\s*\n")
    val lpar = Token("\\(\\s*")
    val rpar = Token("\\)\\s*")
    val comma = Token("\\,\\s*")
    val num = Map( Token("\\d+") ) { it.toInt() }
    val layer = Map(Token("[ML]") * num) { (_, b) -> b }

    fun decl(name : String, c : (Int) -> Expression) : Grammar<Expression> {
        return Map( Skip(Token(name) * ws * eq) * num * Skip(eol) ) { (x,_) -> c(x.second) }
    }
    val viaCost = Map( Skip(Token("ViaCost") * ws * eq) * num * Skip(eol) ) { (x, _) -> ViaCost(x.second) }
    val spacing = decl("Spacing") { c -> Spacing(c) }
    val metalLayers = decl("#MetalLayers") { c -> MetalLayers(c) }
    val routedShapes = decl("#RoutedShapes") { c -> RoutedShapes(c) }
    val routedVias = decl("#RoutedVias") { c -> RoutedVias(c) }
    val obstacles = decl("#Obstacles") { c -> Obstacles(c) }

    val point = Map(lpar * num * comma * num * rpar) {
        Point(it.first.first.first.second, it.first.second)
    }
    val rect = Map(point * point) { (p1, p2) -> Rect(p1,p2) }

    var routedShape = Map(Token("RoutedShape\\s+") * (layer * rect) * eol) { (_, b) -> b }
    var routedVia = Map(Token("RoutedVia\\s+") * (layer * point) * eol) { (_, b) -> b }
    var obstacle = Map(Token("Obstacle\\s+") * (layer * rect) * eol) { (_, b) -> b }


    val expr = "ViaCost = 10\n" +
        "Spacing = 8\n" +
//        "Boundary = (0,0) (7000,3000)\n" +
        "#MetalLayers = 3\n" +
        "#RoutedShapes = 10\n" +
        "#RoutedVias = 3\n" +
        "#Obstacles = 10\n" +
        "RoutedShape M2 (694,482) (700,517)\n" +
        "RoutedShape M3 (743,1704) (790,1746)\n" +
        "RoutedShape M3 (876,2731) (927,2751)\n" +
        "RoutedShape M3 (4768,2513) (4774,2538)\n" +
        "RoutedShape M1 (3288,1903) (3292,1907)\n" +
        "RoutedShape M1 (4522,2640) (4618,2689)\n" +
        "RoutedShape M3 (3155,1724) (3200,1740)\n" +
        "RoutedShape M1 (8,523) (109,660)\n" +
        "RoutedShape M2 (5501,1497) (5638,1530)\n" +
        "RoutedShape M3 (2484,977) (2594,987)\n" +
        "RoutedVia V1 (447,1411)\n" +
        "RoutedVia V1 (4861,2421)\n" +
        "RoutedVia V1 (648,1955)\n" +
        "Obstacle M3 (6985,0) (7000,168)\n" +
        "Obstacle M1 (5134,4) (5233,134)\n" +
        "Obstacle M3 (5760,2813) (6035,2934)\n" +
        "Obstacle M3 (5195,49) (5308,89)\n" +
        "Obstacle M2 (5165,0) (5430,57)\n" +
        "Obstacle M2 (2473,1529) (2733,1632)\n" +
        "Obstacle M3 (1660,2152) (1917,2213)\n" +
        "Obstacle M3 (943,2446) (1187,2505)\n" +
        "Obstacle M1 (769,1568) (781,1650)\n" +
        "Obstacle M1 (5157,1196) (5412,1295)\n"

    println(parseToEnd("RoutedShape M2 (694,482) (700,517)\n", routedShape))
    println(parseToEnd(expr, Many(viaCost + spacing + routedShapes + routedVias + metalLayers + obstacles + routedShape + routedVia + obstacle)))

    val text = "123 456 aaa"

    val txt = Token("\\w+")

    val t2 = num * ws * num * ws * (num + txt)

    val t3 = Sequence(listOf(num, ws, num, ws, (num + txt)))

    val t4 = Many(Map(num * ws) { (a,_) -> a })
    val t5 = Many(Map(num * ws) { (a,_) -> a })

    println(parseToEnd(text, t2))
    println(parseToEnd(text, t3))
    println(parseToEnd("1 2 3 4 5", t4))
    println(parseToEnd("1 2 3 4 5", t5))
}