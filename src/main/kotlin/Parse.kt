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
    val text = "123 456 aaa"

    val ws = Skip(Token("\\s*"))
    val num = Map( Token("\\d+") ) { it.toInt() }
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