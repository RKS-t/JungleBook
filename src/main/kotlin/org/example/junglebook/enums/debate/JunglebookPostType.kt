package org.example.junglebook.enums.debate

enum class JunglebookReferenceType(val value: Int) {
    JUNGLEBOOK(1),
    POST(2),
    REPLY(3);

    companion object {
        fun fromValue(value: Int): JunglebookReferenceType? {
            return values().find { it.value == value }
        }
    }
}