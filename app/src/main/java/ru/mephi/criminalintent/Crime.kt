package ru.mephi.criminalintent

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.DateFormat
import java.util.*

@Entity
data class Crime(@PrimaryKey val id: UUID = UUID.randomUUID(),
                 var title: String =
                     when (Locale.getDefault().language) {
                         "en" -> "New Crime"
                         "ru" -> "Новое преступление"
                         else -> "New Crime"
                     },
                 var date: Date = Date(),
                 var isSolved: Boolean = false,
                 var requiresPolice: Boolean = Random().nextBoolean(),
                 var suspect: String = ""){
    val photoFileName: String
    get() = "IMG_$id.jpg"
}