package com.markish.ajudapp_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"b2.s3.endpoint=https://fake.endpoint",
		"b2.s3.region=fake-region",
		"b2.s3.access-key=fake-key",
		"b2.s3.secret-key=fake-secret",
		"b2.s3.bucket=fake-bucket"
})
class AjudappApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
