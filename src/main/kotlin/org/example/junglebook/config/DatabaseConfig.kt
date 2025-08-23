package org.example.junglebook.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
// 1. @MapperScan을 @EnableJpaRepositories로 변경
@EnableJpaRepositories(basePackages = ["org.example.junglebook.repository"]) // JPA 레포지토리 패키지 경로
@EntityScan(basePackages = ["org.example.junglebook.entity"]) // JPA 엔티티 패키지 경로
@EnableTransactionManagement // 트랜잭션 관리 활성화
class DatabaseConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari.main")
    fun hikariConfig(): HikariConfig {
        return HikariConfig()
    }

    @Bean
    fun dataSource(@Qualifier("hikariConfig") hikariConfig: HikariConfig): DataSource {
        return HikariDataSource(hikariConfig)
    }
}