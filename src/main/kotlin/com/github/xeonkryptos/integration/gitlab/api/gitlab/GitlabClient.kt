package com.github.xeonkryptos.integration.gitlab.api.gitlab

import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings
import jakarta.ws.rs.client.Client
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedTrustManager
import org.glassfish.jersey.client.JerseyClientBuilder

class GitlabClient {

    private val gitlabClientWithoutSslVerification: Client
    private val gitlabClientWithSslVerification: Client

    init { // Create a TrustManager that trusts all certificates
        val trustAllCerts = arrayOf<TrustManager>(object : X509ExtendedTrustManager() {
            override fun getAcceptedIssuers(): Array<X509Certificate>? {
                return null
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) {
            }

            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) {
            }
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        gitlabClientWithoutSslVerification = JerseyClientBuilder.newBuilder().hostnameVerifier { _, _ -> true }.sslContext(sslContext).build()

        gitlabClientWithSslVerification = JerseyClientBuilder.createClient()
    }

    fun getGitlabApiClient(gitlabHostSettings: GitlabHostSettings?): Client {
        return when (gitlabHostSettings?.disableSslVerification) {
            true -> gitlabClientWithoutSslVerification
            else -> gitlabClientWithSslVerification
        }
    }
}