package com.hexix;

import com.hexix.ai.GenerateTextFromTextInput;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class GreetingResourceTest {
    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/status")
          .then()
             .statusCode(200)
             .body(is("Hello from Quarkus REST"));
    }

}
