package org.genzopia.avatar

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform