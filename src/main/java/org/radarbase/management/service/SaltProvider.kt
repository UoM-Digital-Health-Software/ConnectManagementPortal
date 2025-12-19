package org.radarbase.management.service

interface SaltProvider {
    fun getSalt(): String
}
