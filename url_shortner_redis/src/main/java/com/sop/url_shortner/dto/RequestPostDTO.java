package com.sop.url_shortner.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

@Data
public class RequestPostDTO {

    @JsonAlias("longurl")
    private String longUrl;
}
