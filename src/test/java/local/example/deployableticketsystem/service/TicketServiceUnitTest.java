package local.example.deployableticketsystem.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import local.example.deployableticketsystem.entity.Reservation;
import local.example.deployableticketsystem.entity.Ticket;
import local.example.deployableticketsystem.repository.ReservationRepository;
import local.example.deployableticketsystem.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class TicketServiceUnitTest {

  @Mock
  private TicketRepository ticketRepository;

  @Mock
  private ReservationRepository reservationRepository;

  @Mock
  private RedissonClient redisson;

  @Mock
  private PlatformTransactionManager transactionManager;

  @Mock
  private RAtomicLong rAtomicLong;

  @Mock
  private RDeque<String> rDeque;

  private TicketService ticketService;

  @BeforeEach
  void setUp() {
    ticketService = new TicketService(ticketRepository, reservationRepository, redisson,
        transactionManager);
  }

  @Test
  @DisplayName("reserve should succeed when tickets are available")
  void reserve_shouldDecrementRedisAndPushToQueue() {
    // given
    UUID ticketId = UUID.randomUUID();
    String userId = "user-123";

    when(redisson.getAtomicLong("TICKET" + ticketId)).thenReturn(rAtomicLong);
    when(rAtomicLong.decrementAndGet()).thenReturn(99L); // Success
    when(redisson.<String>getDeque("reservation_queue")).thenReturn(rDeque);

    // when
    ticketService.reserve(ticketId, userId);

    // then
    verify(rAtomicLong).decrementAndGet();
    verify(rDeque).addLast(ticketId + ":" + userId);
    verify(ticketRepository, never()).save(any());
  }

  @Test
  @DisplayName("reserve should fail when tickets are sold out")
  void reserve_shouldThrowExceptionAndRevert_whenSoldOut() {
    // given
    UUID ticketId = UUID.randomUUID();
    String userId = "user-123";

    when(redisson.getAtomicLong("TICKET" + ticketId)).thenReturn(rAtomicLong);
    when(rAtomicLong.decrementAndGet()).thenReturn(-1L); // Sold out

    // when & then
    assertThrows(IllegalStateException.class, () -> ticketService.reserve(ticketId, userId));

    verify(rAtomicLong).incrementAndGet();
    verify(redisson, never()).getDeque(anyString());
  }

  @Test
  @DisplayName("consumeReservationQueue should save to DB when queue has items")
  void consumeReservationQueue_shouldProcessItemsAndSaveToDB() {
    // given
    UUID ticketId = UUID.randomUUID();
    String userId = "user-123";
    String item = ticketId + ":" + userId;

    when(redisson.<String>getDeque("reservation_queue")).thenReturn(rDeque);
    when(rDeque.pollFirst()).thenReturn(item, (String) null);
    when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus(true));

    Ticket mockTicket = mock(Ticket.class);
    when(ticketRepository.getReferenceById(ticketId)).thenReturn(mockTicket);

    // when
    ticketService.consumeReservationQueue();

    // then
    verify(ticketRepository).getReferenceById(ticketId);
    verify(reservationRepository).save(any(Reservation.class));
    verify(ticketRepository).decreaseStock(ticketId);
    verify(rDeque, times(2)).pollFirst();
  }

  @Test
  @DisplayName("consumeReservationQueue should do nothing when queue is empty")
  void consumeReservationQueue_shouldDoNothing_whenQueueIsEmpty() {
    // given
    when(redisson.<String>getDeque("reservation_queue")).thenReturn(rDeque);
    when(rDeque.pollFirst()).thenReturn(null);

    // when
    ticketService.consumeReservationQueue();

    // then
    verify(ticketRepository, never()).getReferenceById(any());
    verify(reservationRepository, never()).save(any());
  }

  @Test
  @DisplayName("consumeReservationQueue should retry later when DB fails")
  void consumeReservationQueue_shouldMoveToTailAndBreak_onException() {
    // given
    String item1 = UUID.randomUUID() + ":user1";
    String item2 = UUID.randomUUID() + ":user2";

    when(redisson.<String>getDeque("reservation_queue")).thenReturn(rDeque);
    when(rDeque.pollFirst()).thenReturn(item1, item2);

    when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus(true));
    when(ticketRepository.getReferenceById(any())).thenThrow(
        new RuntimeException("DB Connection Fail"));

    // when
    ticketService.consumeReservationQueue();

    // then
    verify(rDeque).addLast(item1);
    verify(ticketRepository).getReferenceById(any());
    verify(rDeque, times(1)).pollFirst();
  }
}