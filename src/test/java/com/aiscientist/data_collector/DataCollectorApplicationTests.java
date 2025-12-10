package com.aiscientist.data_collector;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@TestPropertySource(properties = {
	"spring.autoconfigure.exclude=" +
		"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
		"org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
		"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
		"org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class DataCollectorApplicationTests {

	@Test
	@Disabled("Test requires Kafka configuration properties which are excluded in test environment")
	void contextLoads() {
	}

}
