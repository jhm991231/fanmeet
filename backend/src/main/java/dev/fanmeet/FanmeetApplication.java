package dev.fanmeet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // @ConfigurationProperties record들을 빈으로 등록
public class FanmeetApplication {

	public static void main(String[] args) {
		SpringApplication.run(FanmeetApplication.class, args);
	}

}
