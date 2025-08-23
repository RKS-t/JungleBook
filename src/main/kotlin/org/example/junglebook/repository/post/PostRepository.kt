package org.example.junglebook.repository.post

import org.example.junglebook.entity.post.PostEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PostRepository : JpaRepository<PostEntity, Long> {

    // 통합 검색 메서드 (원본 selectPageableList 대체)
    @Query("SELECT p FROM PostEntity p WHERE p.boardId = :boardId " +
            "AND p.useYn = true " +
            "AND (:searchType = 0 OR " +
            "     (:searchType = 1 AND (:searchValue IS NULL OR p.title LIKE %:searchValue%)) OR " +
            "     (:searchType = 2 AND (:searchValue IS NULL OR p.content LIKE %:searchValue%)) OR " +
            "     (:searchType = 3 AND (:searchValue IS NULL OR p.nickname LIKE %:searchValue%))) " +
            "ORDER BY p.noticeYn DESC, p.createdDt DESC")
    fun findPageableList(
        @Param("boardId") boardId: Int,
        @Param("searchType") searchType: Int,
        @Param("searchValue") searchValue: String?,
        pageable: Pageable
    ): List<PostEntity>

    // 검색 조건이 포함된 카운트
    @Query("SELECT COUNT(p) FROM PostEntity p WHERE p.boardId = :boardId " +
            "AND p.useYn = true " +
            "AND (:searchType = 0 OR " +
            "     (:searchType = 1 AND (:searchValue IS NULL OR p.title LIKE %:searchValue%)) OR " +
            "     (:searchType = 2 AND (:searchValue IS NULL OR p.content LIKE %:searchValue%)) OR " +
            "     (:searchType = 3 AND (:searchValue IS NULL OR p.nickname LIKE %:searchValue%)))")
    fun countByBoardIdWithSearch(
        @Param("boardId") boardId: Int,
        @Param("searchType") searchType: Int,
        @Param("searchValue") searchValue: String?
    ): Int

    fun findByBoardIdOrderByCreatedDtDesc(boardId: Int, pageable: Pageable): List<PostEntity>

    @Query("SELECT p FROM PostEntity p WHERE p.boardId = :boardId " +
            "AND (:searchValue IS NULL OR p.title LIKE %:searchValue%) " +
            "ORDER BY p.createdDt DESC")
    fun findByBoardIdWithTitleSearch(
        @Param("boardId") boardId: Int,
        @Param("searchValue") searchValue: String?,
        pageable: Pageable
    ): List<PostEntity>

    @Query("SELECT p FROM PostEntity p WHERE p.boardId = :boardId " +
            "AND (:searchValue IS NULL OR p.content LIKE %:searchValue%) " +
            "ORDER BY p.createdDt DESC")
    fun findByBoardIdWithContentSearch(
        @Param("boardId") boardId: Int,
        @Param("searchValue") searchValue: String?,
        pageable: Pageable
    ): List<PostEntity>

    @Query("SELECT p FROM PostEntity p WHERE p.boardId = :boardId " +
            "AND (:searchValue IS NULL OR p.nickname LIKE %:searchValue%) " +
            "ORDER BY p.createdDt DESC")
    fun findByBoardIdWithNicknameSearch(
        @Param("boardId") boardId: Int,
        @Param("searchValue") searchValue: String?,
        pageable: Pageable
    ): List<PostEntity>

    fun countByBoardId(boardId: Int): Int

    override fun findById(id: Long): Optional<PostEntity>

    @Modifying
    @Query("UPDATE PostEntity p SET p.viewCnt = p.viewCnt + 1, p.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :id AND p.boardId = :boardId")
    fun increaseViewCount(@Param("boardId") boardId: Int, @Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE PostEntity p SET p.likeCnt = p.likeCnt + 1, p.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :id AND p.boardId = :boardId")
    fun increaseLikeCount(@Param("boardId") boardId: Int, @Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE PostEntity p SET p.dislikeCnt = p.dislikeCnt + 1, p.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :id AND p.boardId = :boardId")
    fun increaseDislikeCount(@Param("boardId") boardId: Int, @Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE PostEntity p SET p.replyCnt = p.replyCnt + 1, p.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :id AND p.boardId = :boardId")
    fun increaseReplyCount(@Param("boardId") boardId: Int, @Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE PostEntity p SET p.replyCnt = p.replyCnt - 1, p.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :id AND p.boardId = :boardId AND p.replyCnt > 0")
    fun decreaseReplyCount(@Param("boardId") boardId: Int, @Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE PostEntity p SET p.useYn = false, p.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE p.boardId = :boardId AND p.seqNo = :seqNo AND p.userId = :userId")
    fun softDeleteBySeqNo(
        @Param("boardId") boardId: Int,
        @Param("seqNo") seqNo: Long,
        @Param("userId") userId: Long
    ): Int

    fun findByBoardIdAndNoticeYnTrueOrderByCreatedDtDesc(boardId: Int): List<PostEntity>
}