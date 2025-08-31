package com.example.bixi.enums

enum class AttendanceType(val value: String) {
    START("start"),
    STOP("stop");

    override fun toString(): String = value

    companion object {
        fun fromValue(value: String): AttendanceType {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown status: $value")
        }
    }
}
