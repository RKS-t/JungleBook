package org.example.junglebook.aop

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
@Profile("!prod")
class TimeWatchAop {

    // 1. 코틀린 스타일의 로거 선언
    companion object {
        private val log = LoggerFactory.getLogger(TimeWatchAop::class.java)
    }

    @Around("execution(* org.example.junglebook..*Controller.*(..)) && @annotation(org.example.junglebook.annotation.TimeWatch)")
    fun controllerTimeWatchAop(joinPoint: ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()

        // 2. joinPoint.getArgs() 대신 .args 프로퍼티 사용
        val returnVal = joinPoint.proceed(joinPoint.args)

        val estimatedTime = System.currentTimeMillis() - startTime

        // 3. 안전한 캐스팅과 문자열 템플릿 사용
        val requestUri = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)
            ?.request?.requestURI ?: "N/A"

        log.info(
            "requestURI: {}, estimatedTime: {}초",
            requestUri,
            estimatedTime / 1000.0
        )

        return returnVal
    }
}