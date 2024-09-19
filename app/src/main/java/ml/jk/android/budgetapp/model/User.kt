package lasecki.mateusz.kurs.android.budgetapp.model

import com.squareup.moshi.JsonClass
import java.io.Serializable
import java.util.UUID

@JsonClass(generateAdapter=true)
data class User(
    val id: String = UUID.randomUUID().toString(),
    val login:String,
    val email:String,
    val password:String
):Serializable
