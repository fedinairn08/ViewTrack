package com.viewTrack.s3storage;


public interface S3Client {
    void put(S3File s3File, String bucket);

    byte[] get(String filename, String bucket);

    boolean isFileExist(String filename, String bucket);

    void delete(String filename, String bucket);

    void createBucketIfNotExist(String bucket);

}
