package com.study.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class JavaConfig {
	@Bean
	public User user() {
		return new User(1, "chenjinkun", "123456", "186****8882", "china.yunnan.kunming");
	}
}
