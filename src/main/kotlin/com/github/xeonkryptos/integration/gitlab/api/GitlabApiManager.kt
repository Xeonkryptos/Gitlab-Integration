package com.github.xeonkryptos.integration.gitlab.api

import com.github.xeonkryptos.integration.gitlab.api.model.GitlabProject
import com.github.xeonkryptos.integration.gitlab.api.model.GitlabUser
import com.github.xeonkryptos.integration.gitlab.service.AuthenticationManager
import com.github.xeonkryptos.integration.gitlab.service.GitlabSettingsService
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabHostSettings
import com.github.xeonkryptos.integration.gitlab.util.GitlabUtil
import com.intellij.openapi.project.Project
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.core.GenericType
import jakarta.ws.rs.core.MediaType
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl
import org.glassfish.jersey.client.JerseyClientBuilder
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedTrustManager

/**
 * @author Xeonkryptos
 * @since 17.09.2020
 */
class GitlabApiManager(project: Project) {
    private companion object {
        private val LOG = GitlabUtil.LOG
    }

    private val gitlabSettingsService = GitlabSettingsService.getInstance(project)
    private val authenticationManager = AuthenticationManager.getInstance(project)
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

    fun retrieveGitlabUsersFor(gitlabAccounts: Collection<GitlabAccount>): Map<GitlabAccount, GitlabUser> {
        val users = mutableMapOf<GitlabAccount, GitlabUser>()
        gitlabAccounts.filter { authenticationManager.hasAuthenticationTokenFor(it) }.forEach { gitlabAccount ->
            try {
                val gitlabClient = getGitlabApiClient(gitlabAccount)
                val gitlabAccessToken = getToken(gitlabAccount)
                val invocation = gitlabClient.target(gitlabAccount.getGitlabHost()).path("api/v4/user").request().header("PRIVATE-TOKEN", gitlabAccessToken).buildGet()
                users[gitlabAccount] = invocation.invoke(GitlabUser::class.java)
            } catch (e: Exception) {
                LOG.warn("Failed to retrieve user information for gitlab account $gitlabAccount", e)
            }
        }
        return users
    }

    fun retrieveGitlabProjectsFor(gitlabAccounts: Collection<GitlabAccount>): Map<GitlabAccount, List<GitlabProject>> {
        val accountProjects = mutableMapOf<GitlabAccount, List<GitlabProject>>()
        gitlabAccounts.filter { authenticationManager.hasAuthenticationTokenFor(it) }.forEach { gitlabAccount ->
            try {
                val gitlabClient = getGitlabApiClient(gitlabAccount)
                val token = getToken(gitlabAccount)
                val invocation = gitlabClient.target(gitlabAccount.getTargetGitlabHost())
                        .path("api/v4/projects")
                        .queryParam("owned", gitlabAccount.resolveOnlyOwnProjects)
                        .queryParam("membership", gitlabAccount.resolveOnlyOwnProjects)
                        .queryParam("order_by", "id")
                        .queryParam("simple", true) // The required fields are stored in the simple version. Reduces network traffic
                        .queryParam("per_page", 50)
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .header("PRIVATE-TOKEN", token)
                        .buildGet()
                // TODO: Work with pagination. important param: per_page <- contains the number of entries per page
                val genericType: GenericType<List<GitlabProject>> = GenericType<List<GitlabProject>>(ParameterizedTypeImpl(List::class.java, GitlabProject::class.java))
                accountProjects[gitlabAccount] = invocation.invoke(genericType)
            } catch (e: Exception) {
                LOG.warn("Failed to retrieve project information for gitlab account $gitlabAccount", e)
            }
        }
        return accountProjects
    }

    fun loadGitlabUser(gitlabHostSettings: GitlabHostSettings, accessToken: String): GitlabUser {
        val gitlabClient = getGitlabApiClient(gitlabHostSettings)
        val invocation = gitlabClient.target(gitlabHostSettings.gitlabHost).path("api/v4/user").request().header("PRIVATE-TOKEN", accessToken).buildGet()
        return invocation.invoke(GitlabUser::class.java)
    }

    private fun getGitlabApiClient(gitlabAccount: GitlabAccount): Client {
        val targetGitlabHost = gitlabAccount.getTargetGitlabHost()

        return getGitlabApiClient(gitlabSettingsService.state.gitlabHostSettings[targetGitlabHost])
    }

    private fun getGitlabApiClient(gitlabHostSettings: GitlabHostSettings?): Client {
        return when (gitlabHostSettings?.disableSslVerification) {
            true -> gitlabClientWithoutSslVerification
            else -> gitlabClientWithSslVerification
        }
    }

    private fun getToken(gitlabAccount: GitlabAccount): String? {
        val username = gitlabAccount.username
        val gitlabAccessToken by lazy { authenticationManager.getAuthenticationTokenFor(gitlabAccount) }
        if (gitlabAccessToken != null) return gitlabAccessToken
        throw IllegalArgumentException("Cannot access gitlab instance for host ${gitlabAccount.getTargetGitlabHost()} with user $username. Missing access token to authenticate")
    }
}
