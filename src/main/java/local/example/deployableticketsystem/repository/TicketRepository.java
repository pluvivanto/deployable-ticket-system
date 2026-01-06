package local.example.deployableticketsystem.repository;

import java.util.UUID;
import local.example.deployableticketsystem.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

}
