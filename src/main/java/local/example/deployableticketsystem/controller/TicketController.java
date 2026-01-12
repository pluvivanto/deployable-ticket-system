package local.example.deployableticketsystem.controller;

import java.util.UUID;
import local.example.deployableticketsystem.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

  private static final Logger log = LoggerFactory.getLogger(TicketController.class);
  private final TicketService ticketService;

  @Autowired
  public TicketController(TicketService ticketService) {
    this.ticketService = ticketService;
  }

  @PostMapping("/{ticketId}/reserve")
  public ResponseEntity<String> reserveTicket(@PathVariable UUID ticketId,
      @RequestParam String userId) {
    try {
      ticketService.reserve(ticketId, userId);
      return ResponseEntity.accepted().body("Reservation accepted for processing");
    } catch (IllegalStateException e) {
      log.warn("Reservation failed for ticket {} and user {}: {}", ticketId, userId,
          e.getMessage());
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Sold out");
    }
  }
}
