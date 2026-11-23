package com.quickdrop.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:orders")
@AutoConfigureMockMvc
class OrderControllerTest {

	private static final String NEW_ORDER = """
			{"customerName":"Asha","restaurantName":"Spice Hub",
			 "deliveryAddress":"12 MG Road","totalAmount":349.50}
			""";

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper mapper;

	private long createOrder() throws Exception {
		String body = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(NEW_ORDER))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PLACED"))
				.andReturn().getResponse().getContentAsString();
		JsonNode json = mapper.readTree(body);
		return json.get("id").asLong();
	}

	private void move(long id, String target, int expectedStatus) throws Exception {
		mvc.perform(post("/api/orders/" + id + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"" + target + "\"}"))
				.andExpect(status().is(expectedStatus));
	}

	@Test
	void walksTheHappyPathToDelivered() throws Exception {
		long id = createOrder();
		move(id, "ACCEPTED", 200);
		move(id, "PREPARING", 200);
		move(id, "READY", 200);
		move(id, "PICKED_UP", 200);
		move(id, "DELIVERED", 200);

		mvc.perform(get("/api/orders/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DELIVERED"));
	}

	@Test
	void rejectsSkippingAStepWith409() throws Exception {
		long id = createOrder();
		move(id, "DELIVERED", 409);

		mvc.perform(get("/api/orders/" + id))
				.andExpect(jsonPath("$.status").value("PLACED"));
	}

	@Test
	void cannotLeaveATerminalState() throws Exception {
		long id = createOrder();
		move(id, "CANCELLED", 200);
		move(id, "ACCEPTED", 409);
	}

	@Test
	void rejectsInvalidOrder() throws Exception {
		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
						.content("{\"customerName\":\"\",\"restaurantName\":\"X\","
								+ "\"deliveryAddress\":\"Y\",\"totalAmount\":0}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returns404ForUnknownOrder() throws Exception {
		mvc.perform(get("/api/orders/99999")).andExpect(status().isNotFound());
		move(99999, "ACCEPTED", 404);
	}

	@Test
	void filtersByStatus() throws Exception {
		long id = createOrder();
		move(id, "ACCEPTED", 200);

		mvc.perform(get("/api/orders?status=ACCEPTED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id==" + id + ")]").exists());
	}
}
