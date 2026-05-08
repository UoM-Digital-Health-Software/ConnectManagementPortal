package org.radarbase.management.config


import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class YoutubeHeaderFilter : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val req = request as HttpServletRequest
        val res = response as HttpServletResponse

        if (req.requestURI.contains("/api/video-bridge/youtube/")) {


            res.setHeader("Content-Security-Policy",
                "frame-src 'self' https://www.youtube.com https://www.youtube-nocookie.com; " +
                        "frame-ancestors 'self' capacitor://localhost app://localhost http://localhost")

            res.setHeader("X-Frame-Options", "ALLOWALL")
        }

        chain.doFilter(request, response)
    }
}
