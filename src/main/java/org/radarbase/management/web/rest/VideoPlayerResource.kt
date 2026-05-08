package org.radarbase.management.web.rest

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletRequest


@Controller
@RequestMapping("/api/video-bridge")
class VideoPlayerResource {
    @GetMapping("/youtube/{videoId}")
    fun renderPlayer(@PathVariable videoId: String?, model: Model, request: HttpServletRequest): String {
        val origin = buildVideoOrigin(request)
        model.addAttribute("videoId", videoId)
        model.addAttribute("origin", origin)
        return "youtube-player"
    }

    private fun buildVideoOrigin(request: HttpServletRequest): String {
        val scheme = request.scheme
        val serverName = request.serverName
        val serverPort = request.serverPort
        val origin = StringBuilder()
        origin.append(scheme).append("://").append(serverName)


        if (!(scheme == "http" && serverPort == 80 || scheme == "https" && serverPort == 443)) {
            origin.append(":").append(serverPort)
        }
        return origin.toString()
    }
}
