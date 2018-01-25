package org.superbiz.moviefun.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class S3Store implements BlobStore {
    Logger logger = LoggerFactory.getLogger(getClass());

    private final AmazonS3 s3Client;
    private final String s3BucketName;

    public S3Store(AmazonS3 s3Client, String s3BucketName) {
        logger.info("Initializing S3Store [bucket:" + s3BucketName + "]");
        this.s3Client = s3Client;
        this.s3BucketName = s3BucketName;

        if(!s3Client.doesBucketExist(s3BucketName)){
            logger.info("Bucket: " + s3BucketName + " doesn't exist.");
            s3Client.createBucket(s3BucketName);
            logger.info("Bucket: " + s3BucketName + " created.");
        }
    }

    @Override
    public void put(Blob blob) throws IOException {
        logger.info("S3Store.put [bucket:" + s3BucketName + "]");
        ObjectMetadata meta = new ObjectMetadata();
        logger.info("S3Store.put contentType:" + blob.contentType);
        meta.setContentType(blob.contentType);
        s3Client.putObject(s3BucketName, blob.name, blob.inputStream, new ObjectMetadata());

    }

    @Override
    public Optional<Blob> get(String name) throws IOException {
        logger.info("S3Store.get [bucket:" + s3BucketName + "]");
        if(!s3Client.doesObjectExist(s3BucketName, name)){
            logger.info("S3Store.get - return empty Blob");
            return Optional.empty();
        }

        S3Object obj = s3Client.getObject(s3BucketName, name);
        byte[] bytes = IOUtils.toByteArray(obj.getObjectContent());

        logger.info("S3Store.get - return Blob");
        return Optional.of(new Blob(
                name,
                new ByteArrayInputStream(bytes),
                new Tika().detect(bytes)
        ));

    }

    @Override
    public void clean(){
        logger.info("S3Store.clean");
        List<S3ObjectSummary> summaries = s3Client.listObjects(s3BucketName).getObjectSummaries();
        for(S3ObjectSummary summary : summaries){
            s3Client.deleteObject(s3BucketName, summary.getKey());
        }
    }
}
