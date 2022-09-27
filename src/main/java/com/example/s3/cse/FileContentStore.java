package com.example.s3.cse;

import java.util.UUID;

import org.springframework.content.commons.repository.ContentStore;

public interface FileContentStore extends ContentStore<File, UUID> {

}
