package net.unifey.handle

import net.unifey.config.Config
import net.unifey.unifey
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * Manages images with the s3 bucket
 */
object S3ImageHandler {
    /**
     * Get the S3Client (S3 is where images are stored).
     */
    private fun getClient(): S3Client =
            S3Client.builder()
                    .region(Region.US_EAST_2)
                    .build()

    /**
     * Upload an image
     */
    fun upload(key: String, picture: ByteArray) {
        val client = getClient()

        client.putObject(
                PutObjectRequest.builder()
                        .bucket("unifey-cdn")
                        .key(key)
                        .build(),
                RequestBody.fromBytes(picture)
        )

        client.close()
    }

    /**
     * Get an image
     *
     * @param key The key to get.
     * @param defaultKey The key to get if [key] doesn't exist.
     */
    fun getPicture(key: String, defaultKey: String): ByteArray {
        val client = getClient()

        val bytes = try {
            client.getObject(
                    GetObjectRequest.builder()
                            .bucket("unifey-cdn")
                            .key(key)
                            .build(),
                    ResponseTransformer.toBytes()
            ).asByteArray()
        } catch (ex: Exception) {
            client.getObject(
                    GetObjectRequest.builder()
                            .bucket("unifey-cdn")
                            .key(defaultKey)
                            .build(),
                    ResponseTransformer.toBytes()
            ).asByteArray()
        }

        client.close()

        return bytes
    }
}