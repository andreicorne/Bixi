package com.example.bixi.enums

enum class TaskStatus(val value: String) {
    NEW("new"),
    IN_PROGRESS("in_progress"),
    DONE("done"),
    CANCELLED("cancelled"),
    ARCHIVED("archived");

    override fun toString(): String = value

    companion object {
        fun fromValue(value: String): TaskStatus {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown status: $value")
        }
    }
}
