package local.example.deployableticketsystem.repository;

import java.util.UUID;
import local.example.deployableticketsystem.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

  @Modifying
  @Query("UPDATE Ticket t SET t.remainingQuantity = t.remainingQuantity - 1 WHERE t.id = :id AND t.remainingQuantity > 0")
  int decreaseStock(UUID id);

}
