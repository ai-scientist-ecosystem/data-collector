package com.aiscientist.data_collector;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.Mockito.mock;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@TestPropertySource(properties = {
	"spring.autoconfigure.exclude=" +
		"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
		"org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
		"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
		"org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class DataCollectorApplicationTests {

	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		KafkaTemplate<String, Object> mockKafkaTemplate() {
			return mock(KafkaTemplate.class);
		}
	}

	@Test
	void contextLoads() {
	}

}
