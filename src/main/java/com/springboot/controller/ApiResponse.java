package com.springboot.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ApiResponse {
    private String response_msg;

    @JsonCreator
    public ApiResponse(@JsonProperty("response_msg") String response_msg) {
        this.response_msg = response_msg;
    }

}
