package org.example.junglebook.repository.post

import org.example.junglebook.entity.post.BoardEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BoardRepository : JpaRepository<BoardEntity, Int> {

    override fun findAll(): List<BoardEntity>

    // 활성화된 게시판만 조회
    fun findByUseYnTrue(): List<BoardEntity>
}