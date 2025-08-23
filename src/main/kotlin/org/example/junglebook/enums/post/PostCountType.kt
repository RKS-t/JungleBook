package org.example.junglebook.enums.post

enum class PostReferenceType(val value: Int) {
    POST(1),
    REPLY(2);

    companion object {
        fun fromValue(value: Int): PostReferenceType? {
            return values().find { it.value == value }
        }
    }
}

enum class CountType {
    VIEW,
    LIKE,
    DISLIKE,
    POST,
    REPLY
}