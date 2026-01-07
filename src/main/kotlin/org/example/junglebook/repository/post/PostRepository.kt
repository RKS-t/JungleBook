package org.example.junglebook.repository.post

import org.example.junglebook.entity.post.PostEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PostRepository : JpaRepository<PostEntity, Long> {

    @Query("SELECT p FROM PostEntity p WHERE p.boardId = :boardId " +
            "AND p.useYn = true " +
            "AND (:searchType = 0 OR " +
            "     (:searchType = 1 AND (:searchValue IS NULL OR p.title LIKE CONCAT('%', :searchValue, '%'))) OR " +
            "     (:searchType = 2 AND (:searchValue IS NULL OR p.content LIKE CONCAT('%', :searchValue, '%'))) OR " +
            "     (:searchType = 3 AND (:searchValue IS NULL OR p.authorNickname LIKE CONCAT('%', :searchValue, '%')))) " +
            "ORDER BY p.noticeYn DESC, p.createdDt DESC")
    fun findPageableList(
        @Param("boardId") boardId: Int,
        @Param("searchType") searchType: Int,
        @Param("searchValue") searchValue: String?,
        pageable: Pageable
    ): List<PostEntity>

    @Query("SELECT COUNT(p) FROM PostEntity p WHERE p.boardId = :boardId " +
            "AND p.useYn = true " +
            "AND (:searchType = 0 OR " +
            "     (:searchType = 1 AND (:searchValue IS NULL OR p.title LIKE CONCAT('%', :searchValue, '%'))) OR " +
            "     (:searchType = 2 AND (:searchValue IS NULL OR p.content LIKE CONCAT('%', :searchValue, '%'))) OR " +
            "     (:searchType = 3 AND (:searchValue IS NULL OR p.authorNickname LIKE CONCAT('%', :searchValue, '%'))))")
    fun countByBoardIdWithSearch(
        @Param("boardId") boardId: Int,
        @Param("searchType") searchType: Int,
        @Param("searchValue") searchValue: String?
    ): Int

    fun findByBoardIdOrderByCreatedDtDesc(boardId: Int, pageable: Pageable): List<PostEntity>

    @Query("SELECT p FROM PostEntity p WHERE p.boardId = :boardId " +
            "AND (:searchValue IS NULL OR p.title LIKE CONCAT('%', :searchValue, '%')) " +
            "ORDER BY p.createdDt DESC")
    fun findByBoardIdWithTitleSearch(
        @Param("boardId") boardId: Int,
        @Param("searchValue") searchValue: String?,
        pageable: Pageable
    ): List<PostEntity>

    @Query("SELECT p FROM PostEntity p WHERE p.boardId = :boardId " +
            "AND (:searchValue IS NULL OR p.content LIKE CONCAT('%', :searchValue, '%')) " +
            "ORDER BY p.createdDt DESC")
    fun findByBoardIdWithContentSearch(
        @Param("boardId") boardId: Int,
        @Param("searchValue") searchValue: String?,
        pageable: Pageable
    ): List<PostEntity>

    @Query("SELECT p FROM PostEntity p WHERE p.boardId = :boardId " +
            "AND (:searchValue IS NULL OR p.authorNickname LIKE CONCAT('%', :searchValue, '%')) " +
            "ORDER BY p.createdDt DESC")
    fun findByBoardIdWithNicknameSearch(
        @Param("boardId") boardId: Int,
        @Param("searchValue") searchValue: String?,
        pageable: Pageable
    ): List<PostEntity>

    fun countByBoardId(boardId: Int): Int

    fun findByIdAndUseYnTrue(id: Long): PostEntity?

    fun findByBoardIdAndUseYnTrueOrderByNoticeYnDescCreatedDtDesc(
        boardId: Int,
        pageable: Pageable
    ): List<PostEntity>

    fun findByUserIdAndUseYnTrueOrderByCreatedDtDesc(
        userId: Long,
        pageable: Pageable
    ): List<PostEntity>

    @Query("""
        SELECT p FROM PostEntity p 
        WHERE p.boardId = :boardId AND p.useYn = true 
        ORDER BY (p.likeCnt + p.viewCnt) DESC, p.createdDt DESC
    """)
    fun findPopularByBoardId(
        @Param("boardId") boardId: Int,
        pageable: Pageable
    ): List<PostEntity>

    @Query("""
        SELECT p FROM PostEntity p 
        WHERE p.boardId = :boardId AND p.useYn = true 
        AND (:keyword IS NULL OR 
             LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
             LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY p.noticeYn DESC, p.createdDt DESC
    """)
    fun searchByKeyword(
        @Param("boardId") boardId: Int,
        @Param("keyword") keyword: String?,
        pageable: Pageable
    ): List<PostEntity>

    fun countByBoardIdAndUseYnTrue(boardId: Int): Long

    @Modifying
    @Query("UPDATE PostEntity p SET p.viewCnt = p.viewCnt + 1, p.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :id")
    fun increaseViewCount(@Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE PostEntity p SET p.likeCnt = p.likeCnt + 1, p.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :id")
    fun increaseLikeCount(@Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE PostEntity p SET p.likeCnt = GREATEST(0, p.likeCnt - 1), p.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :id")
    fun decreaseLikeCount(@Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE PostEntity p SET p.replyCnt = p.replyCnt + 1, p.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :id")
    fun increaseReplyCount(@Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE PostEntity p SET p.replyCnt = GREATEST(0, p.replyCnt - 1), p.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :id")
    fun decreaseReplyCount(@Param("id") id: Long): Int

    fun findByBoardIdAndNoticeYnTrueAndUseYnTrueOrderByCreatedDtDesc(boardId: Int): List<PostEntity>
}