package org.example.junglebook.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name="board")
data class BoardEntity (

    @Id
    @GeneratedValue
    @Column
    val id: Long? = null,

    @Column
    val title: String




)





