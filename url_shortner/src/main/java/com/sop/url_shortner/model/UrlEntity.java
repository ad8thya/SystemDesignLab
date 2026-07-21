package com.sop.url_shortner.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "urls")
@Data
@NoArgsConstructor
public class UrlEntity {
    @Id
    private String id;

    @Indexed(unique = true)
    private String shortUrl;

    private String longUrl;
}
