import request from '@/utils/request';

export async function ing(id) {
  return request(`/room/${id}`);
}

export async function shutdown(id) {
  return request(`/room/${id}`, {
    method: 'delete',
  });
}

export async function comm(id, commObj) {
  return request(`/room/${id}`, {
    method: 'put',
    data: commObj
  })
}