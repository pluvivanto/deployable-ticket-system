package local.example.deployableticketsystem.controller;

import local.example.deployableticketsystem.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TicketController {

  private final TicketService ticketService;

  @Autowired
  public TicketController(TicketService ticketService) {
    this.ticketService = ticketService;
  }
}
