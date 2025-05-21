package com.example.bixi.services

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonConverterService {

    private val gson = Gson()

    // Serializează obiect într-un JSON string
    fun <T> toJson(data: T): String {
        return gson.toJson(data)
    }

    // Deserializează JSON string într-un obiect generic
    inline fun <reified T> fromJson(json: String): T? {
        return try {
            Gson().fromJson(json, object : TypeToken<T>() {}.type)
        } catch (e: Exception) {
            null
        }
    }
}
