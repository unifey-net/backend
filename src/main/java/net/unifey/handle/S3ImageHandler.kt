package net.unifey.handle

import mu.KotlinLogging
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.regions.Region.US_EAST_2
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/** Manages images with the s3 bucket */
object S3ImageHandler {
    val logger = KotlinLogging.logger {  }

    /** Get the S3Client (S3 is where images are stored). */
    private val client = S3Client.builder().region(US_EAST_2).build()

    /** Upload an image */
    fun upload(key: String, picture: ByteArray) {
        logger.info("Upload S3 Image: $key -> ${picture.size}b")
        client.putObject(
            PutObjectRequest.builder().bucket("unifey-cdn").key(key).build(),
            RequestBody.fromBytes(picture)
        )
    }
    /** Delete an image. */
    fun delete(key: String) {
        logger.info("Delete S3 Image: $key")
        client.deleteObject(DeleteObjectRequest.builder().bucket("unifey-cdn").key(key).build())
    }

    /**
     * Get an image
     *
     * @param key The key to get.
     * @param defaultKey The key to get if [key] doesn't exist.
     */
    fun getPicture(key: String, defaultKey: String): ByteArray {
        logger.info("Obtaining S3 Image: $key (default: $defaultKey)")
        return try {
            client
                .getObject(
                    GetObjectRequest.builder().bucket("unifey-cdn").key(key).build(),
                    ResponseTransformer.toBytes()
                )
                .asByteArray()
        } catch (ex: Exception) {
            client
                .getObject(
                    GetObjectRequest.builder().bucket("unifey-cdn").key(defaultKey).build(),
                    ResponseTransformer.toBytes()
                )
                .asByteArray()
        }
    }
}
