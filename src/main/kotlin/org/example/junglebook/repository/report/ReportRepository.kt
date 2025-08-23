package org.example.junglebook.repository.report

import org.example.junglebook.entity.report.ReportEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReportRepository : JpaRepository<ReportEntity, Long>