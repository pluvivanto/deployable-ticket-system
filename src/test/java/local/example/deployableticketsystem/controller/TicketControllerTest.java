package local.example.deployableticketsystem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import local.example.deployableticketsystem.entity.Ticket;
import local.example.deployableticketsystem.repository.TicketRepository;
import local.example.deployableticketsystem.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TicketService ticketService;

  @MockitoBean
  private TicketRepository ticketRepository;

  @Test
  void createTicket_shouldSaveAndSyncStock() throws Exception {
    Ticket ticket = new Ticket(null, "A", 100L, 10L);
    when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

    mockMvc.perform(post("/api/v1/tickets")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"grade\":\"A\",\"price\":100,\"totalQuantity\":10,\"remainingQuantity\":10}"))
        .andExpect(status().isCreated());

    verify(ticketRepository).save(any(Ticket.class));
    verify(ticketService).syncStock(any(), any());
  }

  @Test
  void getAllTickets_shouldReturnList() throws Exception {
    when(ticketRepository.findAll()).thenReturn(List.of());

    mockMvc.perform(get("/api/v1/tickets"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));

    verify(ticketRepository).findAll();
  }

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
