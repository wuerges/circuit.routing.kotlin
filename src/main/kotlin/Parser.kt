sealed class Parser

data class Partial(val i : Int, val text : String) : Parser()
data class Error(val i : Int, val text : String, val error : String)

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

class Token(text : String) : Grammar<String>() {
    val expr = text.toRegex()

    override fun check(input : Parser) : Pair<String, Parser> {
        when (input) {
            is Partial -> {
                val res = expr.find(input.text, input.i)
                if(res != null) {
                    return Pair(res.value, Partial(input.i+res.value.length, input.text))
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