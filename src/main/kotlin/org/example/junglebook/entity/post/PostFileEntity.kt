package org.example.junglebook.entity.post


import jakarta.persistence.*

@Entity
@Table(name = "post_file")
data class PostFileEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "attach_yn")
    var attachYn: Boolean = false,

    @Column(name = "ref_type")
    val refType: Int? = null,

    @Column(name = "ref_id")
    val refId: Long? = null,

    @Column(name = "user_id")
    val userId: Long? = null,

    @Column(name = "file_type", length = 50)
    val fileType: String? = null,

    @Column(name = "file_size", length = 20)
    val fileSize: String? = null,

    @Column(name = "file_name", length = 255)
    val fileName: String? = null,

    @Column(name = "url", length = 500)
    val url: String? = null
)