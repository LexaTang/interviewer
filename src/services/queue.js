import request from '@/utils/request';

export async function queue() {
  return request(`/get`);
}

export async function enqueue(id) {
  return request(`/enqueue/${id}`);
}

export async function vip(id, room) {
  return request(`/vip/${id}/room/${room}`);
}