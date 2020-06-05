package net.unifey.handle.users

import net.unifey.config.Config
import net.unifey.unifey
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * Manage user's profile pictures.
 */
object ProfilePictureManager {
    private val AWS_ID: String
    private val AWS_SECRET: String

    init {
        val cfg = unifey.getConfigObject<Config>()

        AWS_ID = cfg.awsId ?: ""
        AWS_SECRET = cfg.awsSecret ?: ""
    }

    /**
     * Get the S3Client (S3 is where images are stored).
     */
    private fun getClient(): S3Client =
            S3Client.builder()
                    .credentialsProvider(
                            AwsCredentialsProviderChain.builder()
                                    .addCredentialsProvider { AwsBasicCredentials.create(AWS_ID, AWS_SECRET) }
                                    .build()
                    )
                    .region(Region.US_EAST_2)
                    .build()

    /**
     * Upload a user's profile picture. If a picture already exists, it's overridden.
     */
    fun uploadPicture(user: Long, picture: ByteArray) {
        val client = getClient()

        client.putObject(
                PutObjectRequest.builder()
                        .bucket("unifey-cdn")
                        .key("${user}_pfp.jpg")
                        .build(),
                RequestBody.fromBytes(picture)
        )

        client.close()
    }

    /**
     * Get a user's picture.
     */
    fun getPicture(user: Long): ByteArray {
        val client = getClient()

        val bytes = try {
            client.getObject(
                    GetObjectRequest.builder()
                            .bucket("unifey-cdn")
                            .key("${user}_pfp.jpg")
                            .build(),
                    ResponseTransformer.toBytes()
            ).asByteArray()
        } catch (ex: Exception) {
            client.getObject(
                    GetObjectRequest.builder()
                            .bucket("unifey-cdn")
                            .key("default.jpg")
                            .build(),
                    ResponseTransformer.toBytes()
            ).asByteArray()

        }

        client.close()

        return bytes
    }
}