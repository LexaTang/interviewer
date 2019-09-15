import { Effect } from 'dva';
import { Reducer } from 'redux';

import { ing } from '@/services/room'
import { queue, enqueue, vip } from '@/services/queue'
import { message } from 'antd';

export default {
  namespace: 'queue',

  state: {
    queue: [],
    vip: [],
    rooms: [],
  },

  effects: {
    *fetchRoom({ payload }, { call, put }) {
      let responses = [];
      // eslint-disable-next-line no-undef
      for (let i in ROOMS) {
        // eslint-disable-next-line no-undef
        const room = yield call(ing, ROOMS[i])
        responses.push({...room, id: i});
      }
      yield put({
        type: 'save',
        payload: { ...payload, rooms: responses },
      });
    },
    *fetch(_, { call, put }) {
      const response = yield call(queue);
      yield put({
        type: 'fetchRoom',
        payload: response,
      });
    },
    *enqueue({ id }, { call, put }) {
      yield call(enqueue, id);
      yield put({
        type: 'fetch',
      });
    },
    *envip({ id, room }, { call, put }) {
      yield call(vip, id, room);
      yield put({
        type: 'fetch',
      });
    },
  },

  reducers: {
    save(state, action) {
      return {
        ...state,
        ...action.payload,
      };
    },
  },
};