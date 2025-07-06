package com.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.controller.ApiResponse;
import com.springboot.controller.ApplyOfferRequest;
import com.springboot.controller.ApplyOfferResponse;
import com.springboot.controller.OfferRequest;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

@SpringBootTest
public class CartOfferApplicationTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("cartOfferTestCase")
    public void testE2EScenarios(CartOfferTestCase testCase) throws Exception {
        System.out.println("Running Test case: " + testCase);

        OfferRequest offerRequest = testCase.getOfferRequest();
        String apiResponseAsString = makeRequest(Constants.OFFER_URL, offerRequest, HttpURLConnection.HTTP_OK);
        ApiResponse apiResponse = mapper.readValue(apiResponseAsString, ApiResponse.class);
		Assertions.assertEquals("success", apiResponse.getResponse_msg());

        ApplyOfferRequest applyOfferRequest = testCase.getApplyOfferRequest();
        String applyOfferResponseAsString = makeRequest(Constants.APPLY_OFFER_URL, applyOfferRequest, testCase.getExpectedStatus());
        ApplyOfferResponse applyOfferResponse = mapper.readValue(applyOfferResponseAsString, ApplyOfferResponse.class);
        ApplyOfferResponse expectedResponse = testCase.getApplyOfferResponse();

        Assertions.assertEquals(
            expectedResponse.getCart_value(),
            applyOfferResponse.getCart_value(),
            () -> String.format("Expected Cart value %d but got %d for Test case: %s", expectedResponse.getCart_value(), applyOfferResponse.getCart_value(), testCase)
        );
    }

    public static Stream<CartOfferTestCase> cartOfferTestCase() {
        return Stream.of(
            new CartOfferTestCase(
                "FLAT 10 amount applied correctly",
                new OfferRequest(1, "FLATX", 100, Arrays.asList("p1", "p2")),
                new ApiResponse("success"),
                new ApplyOfferRequest(2000, 1, 1),
                new ApplyOfferResponse(1900),
                HttpURLConnection.HTTP_OK
            ),
            new CartOfferTestCase(
                "FLAT 10% applied correctly",
                new OfferRequest(2, "FLATX%", 10, Collections.singletonList("p1")),
                new ApiResponse("success"),
                new ApplyOfferRequest(2000, 2, 1),
                new ApplyOfferResponse(1800),
                HttpURLConnection.HTTP_OK
            ),
            new CartOfferTestCase(
                "No Offer in user's segment",
                new OfferRequest(3, "FLATX", 100, Arrays.asList("p2", "p3")),
                new ApiResponse("success"),
                new ApplyOfferRequest(2000, 3, 1),
                new ApplyOfferResponse(2000),
                HttpURLConnection.HTTP_OK
            ),
            new CartOfferTestCase(
                "100% discount",
                new OfferRequest(4, "FLATX%", 100, Collections.singletonList("p1")),
                new ApiResponse("success"),
                new ApplyOfferRequest(2000, 4, 1),
                new ApplyOfferResponse(0),
                HttpURLConnection.HTTP_OK
            ),
            new CartOfferTestCase( // Bug
                "Cart value zero",
                new OfferRequest(5, "FLATX", 100, Collections.singletonList("p1")),
                new ApiResponse("success"),
                new ApplyOfferRequest(0, 5, 1),
                new ApplyOfferResponse(0),
                HttpURLConnection.HTTP_OK
            ),
            new CartOfferTestCase( // Bug
                "Value off greater than cart value",
                new OfferRequest(6, "FLATX", 1000, Collections.singletonList("p3")),
                new ApiResponse("success"),
                new ApplyOfferRequest(500, 6, 3),
                new ApplyOfferResponse(0),
                HttpURLConnection.HTTP_OK
            ),
            new CartOfferTestCase( // Bug
                "FLAT Negative amount applied",
                new OfferRequest(7, "FLATX", -100, Arrays.asList("p1", "p2")),
                new ApiResponse("success"),
                new ApplyOfferRequest(2000, 7, 1),
                new ApplyOfferResponse(2000),
                HttpURLConnection.HTTP_OK
            ),
            new CartOfferTestCase( // Bug
                "FLAT Negative percent applied",
                new OfferRequest(8, "FLATX%", -100, Collections.singletonList("p1")),
                new ApiResponse("success"),
                new ApplyOfferRequest(2000, 8, 1),
                new ApplyOfferResponse(2000),
                HttpURLConnection.HTTP_OK
            ),
            new CartOfferTestCase( // Negative case
                "User has no segment",
                new OfferRequest(9, "FLATX", 100, Collections.singletonList("p1")),
                new ApiResponse("success"),
                new ApplyOfferRequest(2000, 9, 100),
                new ApplyOfferResponse(2000),
                HttpURLConnection.HTTP_OK
            ),
            new CartOfferTestCase(
                "User doesn't exist",
                new OfferRequest(10, "FLATX", 100, Arrays.asList("p1", "p2")),
                new ApiResponse("success"),
                new ApplyOfferRequest(2000, 10, 4),
				new ApplyOfferResponse(2000),
                HttpURLConnection.HTTP_FORBIDDEN // returning 200, not sure if this is the expected behaviour
            ),
            new CartOfferTestCase(
                "Offer type doesn't exist",
                new OfferRequest(11, "Random value", 100, Arrays.asList("p1", "p2")),
                new ApiResponse("success"),
                new ApplyOfferRequest(2000, 11, 1),
                new ApplyOfferResponse(2000), // Not sure if this is the expected behaviour, default offer_type is % offer type
                HttpURLConnection.HTTP_OK
            ),
			new CartOfferTestCase(
				"Multiple offer present, Best offer should be picked",
				new OfferRequest(1, "FLATX%", 25, Arrays.asList("p1", "p2")),
				new ApiResponse("success"),
				new ApplyOfferRequest(2000, 1, 1),
				new ApplyOfferResponse(1500), // Picking 1st offer applied instead of best offer
				HttpURLConnection.HTTP_OK
			)
        );
    }

	private String makeRequest(String urlString, Object payload, int expectedStatus) throws Exception {
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestProperty("Content-Type", "application/json");

		String POST_PARAMS = mapper.writeValueAsString(payload);
		OutputStream os = con.getOutputStream();
		os.write(POST_PARAMS.getBytes());
		os.flush();
		os.close();
		int responseCode = con.getResponseCode();
		System.out.println("POST Response Code :: " + responseCode);

		Assert.assertEquals("Expected HTTP status code mismatch! ", expectedStatus, responseCode);
		if (responseCode >= 200 && responseCode < 300) { //success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			// print result
			System.out.println(response);
			return response.toString();
		} else {
			System.out.println("POST request did not work.");
		}
		throw new Exception("Failed to make request");
	}
    public static class Constants {
        public static final String BASE_URL = "http://localhost:9001";

        public static final String OFFER_URL = BASE_URL + "/api/v1/offer";
        public static final String APPLY_OFFER_URL = BASE_URL + "/api/v1/cart/apply_offer";
    }
}
