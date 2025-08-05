package org.example.junglebook.enums


enum class Sex(val desc: String) {
    M ("남자"),
    F ("여자")
    ;
}

enum class Ideology(private val desc: String) {
    C ("CONSERVATION - 보수"),
    L ("LIBERAL - 진보"),
    M ("MODERATE - 중도"),
    N ("NONE = 선택안함 혹은 없음")
    ;
}