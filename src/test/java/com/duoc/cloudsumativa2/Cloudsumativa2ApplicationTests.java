package com.duoc.cloudsumativa2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "ISSUER_URI=https://issuer.example.invalid",
        "AUDIENCE=cloudsumativa2-test"
})
class Cloudsumativa2ApplicationTests {

	@Test
	void contextLoads() {
	}

}
