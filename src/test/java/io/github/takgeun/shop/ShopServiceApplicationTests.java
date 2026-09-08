package io.github.takgeun.shop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "mybatis"})
class ShopServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
