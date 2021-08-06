import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;

import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import static io.restassured.RestAssured.given;

public class OrdersAPITest {
    private Logger log = LoggerFactory.getLogger(OrdersAPITest.class);
    private int orderId=0;

    @Test
    public void createOrderWithoutOrderAt() {
        String createOrderResponse = null;
        double totalDistance;
        try {
            createOrderResponse = createOrderResponse(new String(Files.readAllBytes(Paths.get("src/main/resources/createOrderWithoutOrderAt.json"))), 201);
        } catch (IOException e) {
            log.info("please provide correct payload path location");
        }
        verifyAllFieldsInResponse(createOrderResponse);
        verifyIdIsOfTypeInteger(createOrderResponse);
        totalDistance = isDrivingDistancesInMetersArrayOfIntegers(createOrderResponse);
        isAmountAndCurrencyPresent(createOrderResponse);
        isFareAmountCorrect(createOrderResponse, totalDistance, false);
        isFareCurrencyCorrect(createOrderResponse);
    }

    @Test
    public void createOrderWithOrderAtBetweenTenToFive() {

        String createOrderResponse = null;
        double totalDistance;
        try {
            createOrderResponse = createOrderResponse(new String(Files.readAllBytes(Paths.get("src/main/resources/createOrderWithOrderAt.json"))), 201);
        } catch (IOException e) {
            log.info("please provide correct payload path location");
        }
        verifyAllFieldsInResponse(createOrderResponse);
        verifyIdIsOfTypeInteger(createOrderResponse);
        totalDistance = isDrivingDistancesInMetersArrayOfIntegers(createOrderResponse);
        isAmountAndCurrencyPresent(createOrderResponse);
        isFareAmountCorrect(createOrderResponse, totalDistance, true);
        isFareCurrencyCorrect(createOrderResponse);
    }

    @Test
    public void createOrderErrorForSingleStop() {
        String createOrderResponse = null;
        try {
            createOrderResponse = createOrderResponse(new String(Files.readAllBytes(Paths.get("src/main/resources/createOrderOneStop.json"))), 400);
        } catch (IOException e) {
            log.info("please provide correct payload path location");
        }
        String expectedErrorMessage = "error in field(s): stops";
        verifyErrorMessage(createOrderResponse, expectedErrorMessage);
    }

    @Test
    public void createOrderErrorForIncorrectLatLng() {
        String createOrderResponse = null;
        try {
            createOrderResponse = createOrderResponse(new String(Files.readAllBytes(Paths.get("src/main/resources/createOrderErrorIncorrectLatLng.json"))), 503);
        } catch (IOException e) {
            log.info("please provide correct payload path location");
        }
        String expectedErrorMessage = "Service Unavailable";
        verifyErrorMessage(createOrderResponse, expectedErrorMessage);
    }

    @Test
    public void createOrderErrorForMissingPayload() {
        String createOrderResponse = null;
        try {
            createOrderResponse = createOrderResponse("", 400);
        } catch (Exception e) {
            log.info("please provide correct payload path location");
        }
        String expectedErrorMessage = "";
        verifyErrorMessage(createOrderResponse, expectedErrorMessage);
    }

    @Test(dependsOnMethods = { "createOrderWithoutOrderAt" })
    public void fetchAssigningOrderForCorrectId() {
        double totalDistance = 0;
        String fetchOrderResponse = fetchOrderResponse(orderId, 200);

        verifyAllFieldsInResponse(fetchOrderResponse);
        verifyFetchOrderResponseFields(fetchOrderResponse);
        verifyIdIsOfTypeInteger(fetchOrderResponse);
        verifyStops(fetchOrderResponse);
        totalDistance = isDrivingDistancesInMetersArrayOfIntegers(fetchOrderResponse);
        isAmountAndCurrencyPresent(fetchOrderResponse);
        isFareAmountCorrect(fetchOrderResponse, totalDistance, false);
        isFareCurrencyCorrect(fetchOrderResponse);
        isStatusCorrect(fetchOrderResponse, "ASSIGNING");
        isCreatedDateBeforeTheOrderDate(fetchOrderResponse);
    }

    @Test(dependsOnMethods = { "createOrderWithoutOrderAt","fetchAssigningOrderForCorrectId","takeOrderSuccessfully" })
    public void fetchOngoingOrderForCorrectId() {
        double totalDistance = 0;
        String fetchOrderResponse = fetchOrderResponse(orderId, 200);

        verifyAllFieldsInResponse(fetchOrderResponse);
        verifyFetchOrderResponseFields(fetchOrderResponse);
        verifyIdIsOfTypeInteger(fetchOrderResponse);
        verifyStops(fetchOrderResponse);
        totalDistance = isDrivingDistancesInMetersArrayOfIntegers(fetchOrderResponse);
        isAmountAndCurrencyPresent(fetchOrderResponse);
        isFareAmountCorrect(fetchOrderResponse, totalDistance, false);
        isFareCurrencyCorrect(fetchOrderResponse);
        isStatusCorrect(fetchOrderResponse, "ONGOING");
        isCreatedDateBeforeTheOrderDate(fetchOrderResponse);
    }

    @Test(dependsOnMethods = { "createOrderWithoutOrderAt","cancelOrderSuccessfullyAfterAssigning"})
    public void fetchCancelledOrderForCorrectId() {
        double totalDistance = 0;
        String fetchOrderResponse = fetchOrderResponse(orderId, 200);

        verifyAllFieldsInResponse(fetchOrderResponse);
        verifyFetchOrderResponseFields(fetchOrderResponse);
        verifyIdIsOfTypeInteger(fetchOrderResponse);
        verifyStops(fetchOrderResponse);
        totalDistance = isDrivingDistancesInMetersArrayOfIntegers(fetchOrderResponse);
        isAmountAndCurrencyPresent(fetchOrderResponse);
        isFareAmountCorrect(fetchOrderResponse, totalDistance, false);
        isFareCurrencyCorrect(fetchOrderResponse);
        isStatusCorrect(fetchOrderResponse, "CANCELLED");
        isCreatedDateBeforeTheOrderDate(fetchOrderResponse);
    }

    @Test(dependsOnMethods = { "createOrderWithoutOrderAt","takeOrderSuccessfully","completeOrderSuccessfully"})
    public void fetchCompletedOrderForCorrectId() {
        double totalDistance = 0;
        String fetchOrderResponse = fetchOrderResponse(orderId, 200);

        verifyAllFieldsInResponse(fetchOrderResponse);
        verifyFetchOrderResponseFields(fetchOrderResponse);
        verifyIdIsOfTypeInteger(fetchOrderResponse);
        verifyStops(fetchOrderResponse);
        totalDistance = isDrivingDistancesInMetersArrayOfIntegers(fetchOrderResponse);
        isAmountAndCurrencyPresent(fetchOrderResponse);
        isFareAmountCorrect(fetchOrderResponse, totalDistance, false);
        isFareCurrencyCorrect(fetchOrderResponse);
        isStatusCorrect(fetchOrderResponse, "COMPLETED");
        isCreatedDateBeforeTheOrderDate(fetchOrderResponse);
    }

    @Test
    public void fetchOrderForIncorrectId() {
        String fetchOrderResponse = fetchOrderResponse(1234, 404);
        String expectedErrorMessage = "ORDER_NOT_FOUND";
        verifyErrorMessage(fetchOrderResponse, expectedErrorMessage);
    }

    @Test (dependsOnMethods = { "createOrderWithoutOrderAt" })
    public void takeOrderSuccessfully() {
        String takeOrderResponse = updateOrderResponse(orderId, "take", 200);

        verifyIdIsOfTypeInteger(takeOrderResponse);
        isStatusCorrect(takeOrderResponse, "ONGOING");
        isStatusChangeTimePresent(takeOrderResponse, "ongoingTime");
    }

    @Test(dependsOnMethods = { "createOrderWithoutOrderAt","takeOrderSuccessfully" })
    public void failToTakeOrderForOngoingStatus() {
        String takeOrderResponse = updateOrderResponse(orderId, "take", 422);

        String expectedMessage = "Order status is not ASSIGNING";
        verifyErrorMessage(takeOrderResponse, expectedMessage);
    }

    @Test(dependsOnMethods = { "createOrderWithoutOrderAt", "takeOrderSuccessfully","completeOrderSuccessfully"})
    public void failToTakeOrderForCompletedStatus() {
        String takeOrderResponse = updateOrderResponse(orderId, "take", 422);

        String expectedMessage = "Order status is not ASSIGNING";
        verifyErrorMessage(takeOrderResponse, expectedMessage);
    }

    @Test(dependsOnMethods = { "createOrderWithoutOrderAt","cancelOrderSuccessfullyAfterAssigning"})
    public void failToTakeOrderForCancelledStatus() {
        String takeOrderResponse = updateOrderResponse(orderId, "take", 422);

        String expectedMessage = "Order status is not ASSIGNING";
        verifyErrorMessage(takeOrderResponse, expectedMessage);
    }

    @Test
    public void failToTakeOrderForIncorrectOrderId() {
        String takeOrderResponse = updateOrderResponse(1234, "take", 404);

        String expectedMessage = "ORDER_NOT_FOUND";
        verifyErrorMessage(takeOrderResponse, expectedMessage);
    }

    @Test(dependsOnMethods = { "createOrderWithoutOrderAt","takeOrderSuccessfully" })
    public void completeOrderSuccessfully() {
        String takeOrderResponse = updateOrderResponse(orderId, "complete", 200);

        verifyIdIsOfTypeInteger(takeOrderResponse);
        isStatusCorrect(takeOrderResponse, "COMPLETED");
        isStatusChangeTimePresent(takeOrderResponse, "completedAt");
    }

    @Test
    public void failToCompleteOrderForIncorrectOrderId() {
        String takeOrderResponse = updateOrderResponse(1234, "complete", 404);

        String expectedMessage = "ORDER_NOT_FOUND";
        verifyErrorMessage(takeOrderResponse, expectedMessage);
    }

    @Test(dependsOnMethods = { "createOrderWithoutOrderAt" })
    public void failToCompleteOrderForAnyInvalidStatus() {
        String takeOrderResponse = updateOrderResponse(orderId, "complete", 422);

        String expectedMessage = "Order status is not ONGOING";
        verifyErrorMessage(takeOrderResponse, expectedMessage);
    }

    @Test(dependsOnMethods = { "createOrderWithoutOrderAt" })
    public void cancelOrderSuccessfullyAfterAssigning() {
        String takeOrderResponse = updateOrderResponse(orderId, "cancel", 200);

        verifyIdIsOfTypeInteger(takeOrderResponse);
        isStatusCorrect(takeOrderResponse, "CANCELLED");
        isStatusChangeTimePresent(takeOrderResponse, "cancelledAt");
    }

    @Test(dependsOnMethods = { "createOrderWithoutOrderAt","takeOrderSuccessfully" })
    public void cancelOrderSuccessfullyAfterOngoing() {
        String takeOrderResponse = updateOrderResponse(orderId, "cancel", 200);

        verifyIdIsOfTypeInteger(takeOrderResponse);
        isStatusCorrect(takeOrderResponse, "CANCELLED");
        isStatusChangeTimePresent(takeOrderResponse, "cancelledAt");
    }

    @Test
    public void failToCancelOrderForIncorrectOrderId() {
        String takeOrderResponse = updateOrderResponse(1234, "cancel", 404);

        String expectedMessage = "ORDER_NOT_FOUND";
        verifyErrorMessage(takeOrderResponse, expectedMessage);
    }

    @Test(dependsOnMethods = { "createOrderWithoutOrderAt","takeOrderSuccessfully","completeOrderSuccessfully" })
    public void failToCancelOrderForCompletedStatus() {
        String takeOrderResponse = updateOrderResponse(orderId, "cancel", 422);

        String expectedMessage = "Order status is COMPLETED already";
        verifyErrorMessage(takeOrderResponse, expectedMessage);
    }

    public String createOrderResponse(String path, int expectedStatusCode) {
        RestAssured.baseURI = "http://localhost:51544";

        return given().log().all()
                .header("Content-Type", "application/json")
                .body(path)
                .when().post("/v1/orders")
                .then().log().all()
                .assertThat().statusCode(expectedStatusCode)
                .extract().asString();
    }

    public String fetchOrderResponse(int id, int expectedStatusCode) {
        RestAssured.baseURI = "http://localhost:51544";

        return given().log().all()
                .when().get("/v1/orders/" + id)
                .then().log().all()
                .assertThat().statusCode(expectedStatusCode)
                .extract().asString();
    }

    public String updateOrderResponse(int id, String action, int expectedStatusCode) {
        RestAssured.baseURI = "http://localhost:51544";

        return given().log().all()
                .header("Content-Type", "application/json")
                .when().put("/v1/orders/" + id + "/" + action)
                .then().log().all()
                .assertThat().statusCode(expectedStatusCode)
                .extract().asString();
    }

    public void verifyErrorMessage(String orderResponse, String expectedErrorMessage) {
        //verify error message is correct
        JsonPath jsonPath = new JsonPath(orderResponse);
        Boolean condition = jsonPath.getString("message").equalsIgnoreCase(expectedErrorMessage);
        String reasonForFailure = "Error message is incorrect in response";
        Assert.assertTrue(condition, reasonForFailure);
    }

    public Boolean isFareCalculationCorrect(Double amount, double totalDistance, boolean isOddHours) {
        //check if correct fare calculated depending on the order place time
        double tolerance = 0.01;
        double calculatedFareAmount = 20 + (totalDistance - 2000) / 200 * 5;
        double oddHoursCalculatedFareAmount = 30 + (totalDistance - 2000) / 200 * 8;
        if (!isOddHours)
            return (amount - calculatedFareAmount) < tolerance;
        else
            return (amount - oddHoursCalculatedFareAmount) < tolerance;
    }

    public void verifyAllFieldsInResponse(String orderResponse) {
        //check if all fields are present in response
        Boolean condition = orderResponse.contains("id") &&
                orderResponse.contains("drivingDistancesInMeters") &&
                orderResponse.contains("fare");
        String reasonForFailure = "All fields are not present in response";
        Assert.assertTrue(condition, reasonForFailure);
    }

    public void verifyFetchOrderResponseFields(String orderResponse) {
        //check if all fields are present in response
        Boolean condition = orderResponse.contains("stops") &&
                orderResponse.contains("createdTime") &&
                orderResponse.contains("orderDateTime") &&
                orderResponse.contains("status");
        String reasonForFailure = "All fields are not present in fetch order response";
        Assert.assertTrue(condition, reasonForFailure);
    }

    public void verifyIdIsOfTypeInteger(String orderResponse) {
        //check if Id is of type Integer
        JSONObject jsonObject = new JSONObject(orderResponse);
        Object object = jsonObject.get("id");
        orderId =  jsonObject.getInt("id");
        Boolean condition = object instanceof Integer;
        String reasonForFailure = "Id is not of type integer";
        Assert.assertTrue(condition, reasonForFailure);
    }

    public double isDrivingDistancesInMetersArrayOfIntegers(String orderResponse) {
        //check if drivingDistancesInMeters is array of integers
        JSONArray jsonArray = new JSONObject(orderResponse).getJSONArray("drivingDistancesInMeters");
        double totalDistance = 0;
        for (int i = 0; i < jsonArray.length(); i++) {
            Boolean condition = jsonArray.get(i) instanceof Integer;
            String reasonForFailure = "distance is not of type integer";
            Assert.assertTrue(condition, reasonForFailure);
            totalDistance += jsonArray.getInt(i);
        }
        return totalDistance;
    }

    public void isAmountAndCurrencyPresent(String orderResponse) {
        //check if fare contains amount and currency
        JsonPath jsonPath = new JsonPath(orderResponse);
        Boolean condition = !jsonPath.getString("fare.amount").isEmpty() &&
                !jsonPath.getString("fare.currency").isEmpty();
        String reasonForFailure = "amount or currency is not present in response";
        Assert.assertTrue(condition, reasonForFailure);
    }

    public void isFareAmountCorrect(String orderResponse, double totalDistance, boolean isOddHours) {
        //check if fare amount is calculated correctly
        Boolean condition = isFareCalculationCorrect(Double.parseDouble(new JsonPath(orderResponse).getString("fare.amount")), totalDistance, isOddHours);
        String reasonForFailure = "fare amount is not correct in response";
        Assert.assertTrue(condition, reasonForFailure);
    }

    public void isFareCurrencyCorrect(String orderResponse) {
        //check if fare currency is in HKD
        Boolean condition = new JsonPath(orderResponse).getString("fare.currency").equalsIgnoreCase("HKD");
        String reasonForFailure = "amount currency is not HKD";
        Assert.assertTrue(condition, reasonForFailure);
    }

    public void isStatusCorrect(String orderResponse, String status) {
        //check if order status is correct
        Boolean condition = new JsonPath(orderResponse).getString("status").equalsIgnoreCase(status);
        String reasonForFailure = "Order status is not correct";
        Assert.assertTrue(condition, reasonForFailure);
    }

    public void isCreatedDateBeforeTheOrderDate(String orderResponse) {
        //check if created date is before order date is correct
        JsonPath jsonPath = new JsonPath(orderResponse);
        Date createdTime = null, orderDateTime = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        try {
            createdTime = sdf.parse(jsonPath.getString("createdTime"));
            orderDateTime = sdf.parse(jsonPath.getString("orderDateTime"));
        } catch (ParseException e) {
            log.info("not able to parse date");
        }
        Boolean condition = createdTime.before(orderDateTime) || createdTime.equals(orderDateTime);
        String reasonForFailure = "Created date and time is not same or before order date time";
        Assert.assertTrue(condition, reasonForFailure);
    }

    public void verifyStops(String orderResponse) {
        //check if stops is an array and has lat and lng and is of type big decimal
        JSONArray jsonArray = new JSONObject(orderResponse).getJSONArray("stops");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject json = jsonArray.getJSONObject(i);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Boolean condition = (key.contains("lat") || key.contains("lng")) &&
                        (json.get(key) instanceof BigDecimal);
                String reasonForFailure = "stops does not contain lat and lng";
                Assert.assertTrue(condition, reasonForFailure);
            }
        }
    }

    public void isStatusChangeTimePresent(String orderResponse, String key) {
        //check if status change time is not empty
        try {
            String time = new JsonPath(orderResponse).getString(key);
            Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(time);
            Boolean condition = !time.isEmpty();
            String reasonForFailure = "status change time is empty";
            Assert.assertTrue(condition, reasonForFailure);
        } catch (ParseException e) {
            log.info("not able to parse date");
            Assert.fail("date format is incorrect");
        }
    }
}