import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 200,
  duration: '30s',
};

export default function () {
  const ticketId = __ENV.TICKET_ID;
  if (!ticketId) {
    throw new Error('TICKET_ID environment variable is required');
  }

  const userId = `user_${__VU}_${__ITER}`;
  const res = http.post(`http://localhost:8080/api/v1/tickets/${ticketId}/reserve?userId=${userId}`, null, {
    tags: { name: 'ReserveTicket' },
  });
  
  check(res, {
    'status is 202 or 409': (r) => r.status === 202 || r.status === 409,
  });

  sleep(0.01);
}
