package org.example.junglebook.filter

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.apache.commons.io.IOUtils
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * HTTP 요청의 본문(Body)을 여러 번 읽을 수 있도록 원본 요청을 감싸는 래퍼 클래스.
 * 생성 시점에 요청 본문을 byte 배열로 캐싱합니다.
 */
class HttpRequestWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

    private val bodyData: ByteArray

    init {
        // 생성자 초기화 시점에 원본 스트림을 읽어 byte 배열에 캐싱합니다.
        bodyData = IOUtils.toByteArray(super.getInputStream())
    }

    override fun getInputStream(): ServletInputStream {
        // 캐싱된 데이터를 기반으로 매번 새로운 스트림을 생성하여 반환합니다.
        val byteArrayInputStream = ByteArrayInputStream(bodyData)
        return ServletInputStreamImpl(byteArrayInputStream)
    }
}

/**
 * ServletInputStream 추상 클래스를 구현한 헬퍼 클래스.
 * 일반 InputStream을 ServletInputStream처럼 동작하게 합니다.
 */
private class ServletInputStreamImpl(private val inputStream: InputStream) : ServletInputStream() {

    // 코틀린의 '표현식 본문'을 사용하여 코드를 한 줄로 줄입니다.
    override fun read(): Int = inputStream.read()

    override fun read(b: ByteArray): Int = inputStream.read(b)

    // 비동기 처리를 사용하지 않으므로 기본값을 반환합니다.
    override fun isFinished(): Boolean = true

    override fun isReady(): Boolean = true

    override fun setReadListener(listener: ReadListener) {
        throw UnsupportedOperationException("Not implemented")
    }
}