package org.example.junglebook.filter

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class HttpRequestWrapperFilter : Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        // jakarta.servlet.Filter의 init, destroy는 default 메서드이므로 구현이 필요 없으면 생략 가능

        val httpServletRequest = request as HttpServletRequest

        // contentType이 multipart/form-data 이거나 text/html인 경우 본문을 캐싱하지 않음
        // (파일 업로드 등 대용량 데이터 메모리 적재 방지)
        when {
            httpServletRequest.contentType?.startsWith("multipart/form-data") == true ||
                    httpServletRequest.contentType?.startsWith("text/html") == true -> {
                chain.doFilter(httpServletRequest, response)
            }
            else -> {
                val requestWrapper = HttpRequestWrapper(httpServletRequest)
                chain.doFilter(requestWrapper, response)
            }
        }
    }
}