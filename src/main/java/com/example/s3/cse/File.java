package com.example.s3.cse;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class File {

    @Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

    private String name;

    @JsonIgnore
    private byte[] contentKey;

	@ContentId private UUID contentId;
	@ContentLength private long contentLength;
	@MimeType private String contentMimeType;
}