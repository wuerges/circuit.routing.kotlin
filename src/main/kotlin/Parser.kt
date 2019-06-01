sealed class Parser

data class Partial(val i : Int, val text : String) : Parser()
data class Error(val i : Int, val text : String, val error : String) : Parser()

sealed class Either<A, B>
class Left<A, B>(val left: A) : Either<A, B>()
class Right<A, B>(val right: B) : Either<A, B>()

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
        val (a1, b1) = g1.check(input)
        when(b1) {
            is Partial -> {
                return Pair(Left(a1), b1)
            }
        }
        val (a2, b2) = g2.check(input)
        when(b2) {
            is Partial -> {
                return Pair(Right(a2), b2)
            }
        }
        return Pair(Left(a1), b2)
    }
}

class Token(val text : String) : Grammar<String>() {
    val expr = text.toRegex()

    override fun check(input : Parser) : Pair<String, Parser> {
        when (input) {
            is Partial -> {
                val res = expr.find(input.text, input.i)
                if(res != null) {
                    return Pair(res.value, Partial(input.i+res.value.length, input.text))
                }
                else {
                    return Pair("", Error(input.i, input.text, "Could not match $text"))
                }
            }
        }
        return Pair("", input)
    }
}


fun main() {
    val x = Partial(0, "123 mundo")

    val t = Map(Token("\\d+"), { it.toInt() })
    println(t.check(x))
}