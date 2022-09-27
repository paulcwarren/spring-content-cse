package com.example.s3.cse;

import java.util.UUID;

import javax.persistence.Embeddable;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Content {

  @ContentId private UUID contentId;
  @ContentLength private long contentLength;
  @MimeType private String contentMimeType;

}
