package com.example.bixi.helper

import com.example.bixi.enums.FieldType
import com.example.bixi.models.Validator

object ValidatorHelper {
    fun getValidatorsFor(fieldType: FieldType): List<Validator> {
        return when (fieldType) {
            FieldType.EMAIL -> listOf(
                Validator(
                    Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"),
                    "Folosește un format corect, ex: nume@exemplu.com"
                ),
                Validator(Regex(".{8,}"), "Folosește un format corect, ex: nume@exemplu.com")
            )

            FieldType.LOGIN_PASSWORD -> listOf(
                Validator(
                    Regex("^.{5,100}$"), // asta ii sa contina si o majuscula -> Regex("^(?=.*[A-Z]).{5,100}\$\n"),
                    "Parola trebuie să aibă între 5 și 100 de caractere" // "Parola trebuie să aibă între 5 și 100 de caractere și să conțină cel puțin o literă mare"
                )
            )

            FieldType.TASK_TITLE -> listOf(
                Validator(
                    Regex("^.{2,50}\$"),
                    "Titlul trebuie să aibă între 2 și 50 de caractere"
                )
            )

            FieldType.TASK_DETAILS -> listOf(
                Validator(
                    Regex("^[\\s\\S]{0,5000}\$"),
                    "Descrierea trebuie să aibă maximum 5000 de caractere"
                )
            )

            FieldType.TASK_RESPONSIBLE -> listOf(
                Validator(
                    Regex("^.{2,}\$"),
                    "Alegeți un responsabil"
                )
            )

            FieldType.TASK_START_DATE -> listOf(
                Validator(
                    Regex("^.{2,}\$"),
                    "Alegeți o dată de început"
                )
            )

            FieldType.TASK_END_DATE -> listOf(
                Validator(
                    Regex("^.{2,}\$"),
                    "Alegeți o dată de sfârșit"
                )
            )
        }
    }
}