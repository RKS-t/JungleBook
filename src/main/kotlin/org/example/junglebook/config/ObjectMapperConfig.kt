package org.example.junglebook.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class ObjectMapperConfig {

    @Bean
    fun jackson2ObjectMapperBuilder(): Jackson2ObjectMapperBuilder {
        val module = JavaTimeModule()
        return Jackson2ObjectMapperBuilder()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // ZonedDateTime을 ISO-8601 형식으로 직렬화
            .failOnUnknownProperties(false) // 모르는 JSON 필드는 무시
            .serializationInclusion(JsonInclude.Include.NON_NULL) // Null 값은 포함하지 않음
            .modulesToInstall(module) // Java 8 날짜/시간 타입 지원
    }
}