package app.myroutine.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter fun intsToString(v: List<Int>): String = json.encodeToString(ListSerializer(Int.serializer()), v)
    @TypeConverter fun stringToInts(v: String): List<Int> = json.decodeFromString(ListSerializer(Int.serializer()), v)

    @TypeConverter fun longsToString(v: List<Long>): String = json.encodeToString(ListSerializer(Long.serializer()), v)
    @TypeConverter fun stringToLongs(v: String): List<Long> = json.decodeFromString(ListSerializer(Long.serializer()), v)

    @TypeConverter fun stringsToString(v: List<String>): String = json.encodeToString(ListSerializer(String.serializer()), v)
    @TypeConverter fun stringToStrings(v: String): List<String> = json.decodeFromString(ListSerializer(String.serializer()), v)
}
