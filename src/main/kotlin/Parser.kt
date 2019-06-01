import java.lang.Exception

class Error(val i : Int, val text : String, error : String) : Exception(error)

class Parser(val i : Int, val text : String) {
    fun fail(message :String) : Error {
        return Error(i, text, message)
    }

    override fun toString() : String {
        val a = text.substring(0, i)
        val b = text.substring(i)
        return "Parser{$i, \"$a\", \"$b\"}"
    }
}

sealed class Either<A, B>
data class Left<A, B>(val left: A) : Either<A, B>()
data class Right<A, B>(val right: B) : Either<A, B>()

abstract class Grammar<T> {
    abstract fun check(input : Parser) : Pair<T, Parser>
}

class Map<T1, T2>(val grammar: Grammar<T1>, val f :(p: T1) -> T2 ) : Grammar<T2>()
{
    override fun check(input : Parser) : Pair<T2, Parser> {
        val (a, b) = grammar.check(input)
        return Pair(f(a), b)
    }
}

class Or<T1, T2>(val g1: Grammar<T1>, val g2: Grammar<T2>) : Grammar<Either<T1, T2>>()
{
    override fun check(input: Parser) : Pair<Either<T1, T2>, Parser>{

        try {
            val (a1, b1) = g1.check(input)
            return Pair(Left(a1), b1)
        }
        catch (e : Error) {
            try {
                val (a2, b2) = g2.check(input)
                return Pair(Right(a2), b2)
            }
            catch(e2 : Error) {
                throw input.fail(e.message + " <+or+> " + e2.message)
            }
        }
    }
}

class And<T1, T2>(val g1: Grammar<T1>, val g2: Grammar<T2>) : Grammar<Pair<T1, T2>>()
{
    override fun check(input: Parser) : Pair<Pair<T1, T2>, Parser>{
        val (a1, b1) = g1.check(input)
        val (a2, b2) = g2.check(b1)
        return Pair(Pair(a1, a2), b2)
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

    override fun check(input : Parser) : Pair<String, Parser> {
        val res = expr.find(input.text, input.i)
        if(res != null) {
            return Pair(res.value, Parser(input.i+res.value.length, input.text))
        }
        throw input.fail("Could not match $text")
    }
}


fun main() {
    val x = Parser(0, "123 456 aaa")

    val ws = Token("\\s*")

    val num = Map(Token("\\d+"), { it.toInt() })
    val txt = Token("\\w+")

    val t2 = num * ws * num * ws * (num + txt)


    println(t2.check(x))
}