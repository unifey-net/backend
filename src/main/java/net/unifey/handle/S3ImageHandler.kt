package net.unifey.handle

/** Manages images with the s3 bucket */
object S3ImageHandler {
    //    /**
    //     * Get the S3Client (S3 is where images are stored).
    //     */
    //    private val client = S3Client.builder()
    //            .region(Region.US_EAST_2)
    //            .build()

    //    /**
    //     * Upload an image
    //     */
    //    fun upload(key: String, picture: ByteArray) {
    //        client.putObject(
    //                PutObjectRequest.builder()
    //                        .bucket("unifey-cdn")
    //                        .key(key)
    //                        .build(),
    //                RequestBody.fromBytes(picture)
    //        )
    //    }

    fun upload(key: String, picture: ByteArray) {}

    //    /**
    //     * Delete an image.
    //     */
    //    fun delete(key: String) {
    //        client.deleteObject(
    //                DeleteObjectRequest.builder()
    //                        .bucket("unifey-cdn")
    //                        .key(key)
    //                        .build()
    //        )
    //    }

    fun delete(key: String) {}

    //    /**
    //     * Get an image
    //     *
    //     * @param key The key to get.
    //     * @param defaultKey The key to get if [key] doesn't exist.
    //     */
    //    fun getPicture(key: String, defaultKey: String): ByteArray {
    //        return try {
    //            client.getObject(
    //                    GetObjectRequest.builder()
    //                            .bucket("unifey-cdn")
    //                            .key(key)
    //                            .build(),
    //                    ResponseTransformer.toBytes()
    //            ).asByteArray()
    //        } catch (ex: Exception) {
    //            client.getObject(
    //                    GetObjectRequest.builder()
    //                            .bucket("unifey-cdn")
    //                            .key(defaultKey)
    //                            .build(),
    //                    ResponseTransformer.toBytes()
    //            ).asByteArray()
    //        }
    //    }

    fun getPicture(key: String, defaultKey: String): ByteArray {
        return javaClass.classLoader.getResourceAsStream("default_picture.png")?.readBytes()
            ?: ByteArray(0)
    }
}
