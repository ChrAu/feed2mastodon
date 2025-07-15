package com.hexix;

import com.hexix.ai.GenerateTextFromTextInput;
import com.hexix.ai.ThemenEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

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
    @BeforeEach
    @Transactional
    void  setUp() {
        ThemenEntity.<ThemenEntity>findAll().stream().filter(themenEntity -> "Pilze".equals(themenEntity.getThema())).findFirst().ifPresent(PanacheEntityBase::delete);
    }

    @Test
    @Disabled
    void testPutThemen(){
        given().contentType(ContentType.TEXT).body("Pilze").when().post("/themen").then().statusCode(200).body("thema", is("Pilze")).body("uuid", notNullValue());
    }

}
