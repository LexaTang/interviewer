import request from '@/utils/request';

export async function info(id) {
  return request(`/info/${id}`);
}
