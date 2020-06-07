package net.unifey.config

data class Config(
        val username: String? = null,
        val url: String? = null,
        val password: String? = null,
        val awsId: String? = null,
        val awsSecret: String? = null,
        val smtpHost: String? = null,
        val smtpUsername: String? = null,
        val smtpPassword: String? = null
)