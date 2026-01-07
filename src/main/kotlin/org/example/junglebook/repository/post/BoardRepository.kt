package org.example.junglebook.repository.post

import org.example.junglebook.entity.post.BoardEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BoardRepository : JpaRepository<BoardEntity, Int> {

    override fun findAll(): List<BoardEntity>

    fun findByUseYnTrue(): List<BoardEntity>
}