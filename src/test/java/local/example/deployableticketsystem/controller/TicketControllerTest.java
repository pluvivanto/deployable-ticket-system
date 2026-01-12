package local.example.deployableticketsystem.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import local.example.deployableticketsystem.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TicketService ticketService;

  @Test
  void reserveTicket_shouldReturnAccepted_whenSuccessful() throws Exception {
    // given
    UUID ticketId = UUID.randomUUID();
    String userId = "user1";

    // when & then
    mockMvc.perform(post("/api/v1/tickets/{ticketId}/reserve", ticketId)
            .param("userId", userId))
        .andExpect(status().isAccepted())
        .andExpect(content().string("Reservation accepted for processing"));

    verify(ticketService).reserve(ticketId, userId);
  }

  @Test
  void reserveTicket_shouldReturnConflict_whenSoldOut() throws Exception {
    // given
    UUID ticketId = UUID.randomUUID();
    String userId = "user1";

    doThrow(new IllegalStateException("Sold out"))
        .when(ticketService).reserve(ticketId, userId);

    //when & then
    mockMvc.perform(post("/api/v1/tickets/{ticketId}/reserve", ticketId)
            .param("userId", userId))
        .andExpect(status().isConflict())
        .andExpect(content().string("Sold out"));
  }
}
