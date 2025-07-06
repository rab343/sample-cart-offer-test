package com.springboot.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ApplyOfferResponse {
    private int cart_value;

    @JsonCreator
    public ApplyOfferResponse(@JsonProperty("cart_value") int cart_value) {
        this.cart_value = cart_value;
    }
}
