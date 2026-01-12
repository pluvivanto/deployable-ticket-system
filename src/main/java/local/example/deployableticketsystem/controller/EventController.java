package local.example.deployableticketsystem.controller;

import java.net.URI;
import local.example.deployableticketsystem.entity.Event;
import local.example.deployableticketsystem.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

  private final EventRepository eventRepository;

  @Autowired
  public EventController(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  @PostMapping
  public ResponseEntity<Event> createEvent(@RequestBody Event event) {
    Event savedEvent = eventRepository.save(event);
    return ResponseEntity.created(URI.create("/api/v1/events/" + savedEvent.getId()))
        .body(savedEvent);
  }
}
