package com.springboot;

import com.springboot.controller.ApiResponse;
import com.springboot.controller.ApplyOfferRequest;
import com.springboot.controller.ApplyOfferResponse;
import com.springboot.controller.OfferRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartOfferTestCase {

    private static final String a = "";

    private String description;

    private OfferRequest offerRequest;
    private ApiResponse apiResponse;

    private ApplyOfferRequest applyOfferRequest;
    private ApplyOfferResponse applyOfferResponse;

    private int expectedStatus;

    @Override
    public String toString() {
        return description;
    }
}
