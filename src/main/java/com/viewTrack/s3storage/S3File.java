package com.viewTrack.s3storage;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class S3File {
    private String filename;
    private UUID uploadId;
    private byte[] content;

    public S3File(String filename, byte[] content) {
        this.filename = filename;
        this.content = content;
        this.uploadId = UUID.randomUUID();
    }

    public S3File(String filename) {
        this.filename = filename;
    }

    public S3File(byte[] content) {
        this.content = content;
    }
}
