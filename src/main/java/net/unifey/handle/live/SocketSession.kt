package net.unifey.handle.live

import io.ktor.http.cio.websocket.*
import kotlin.jvm.Throws
import kotlin.reflect.KClass
import net.unifey.auth.tokens.Token
import net.unifey.handle.InvalidArguments
import org.json.JSONArray
import org.json.JSONObject

data class SocketSession(val session: WebSocketSession, val data: JSONObject, val token: Token) {
    /** Check if [data] contains all of [arg] */
    fun checkArguments(vararg arg: Pair<String, KClass<*>>) {
        arg.forEach { pair ->
            val (name, clazz) = pair

            if (!data.has(name) || data[name]::class != clazz) throw InvalidArguments(name)
        }
    }

    @Throws(InvalidArguments::class)
    inline fun <reified T : Any> getListArgument(arg: String): List<T> {
        if (!data.has(arg) || data[arg]::class != JSONArray::class) throw InvalidArguments(arg)

        val list = (data[arg]!! as JSONArray).toList()

        return when {
            list.isEmpty() -> emptyList()
            list[0]!!::class == T::class -> list as List<T>
            else -> throw InvalidArguments(arg)
        }
    }
}
