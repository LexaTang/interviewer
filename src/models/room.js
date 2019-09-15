import { Effect } from 'dva';
import { Reducer } from 'redux';

import { ing, comm, shutdown } from '@/services/room'
import { info } from '@/services/info'
import { message } from 'antd';

export default {
  namespace: 'room',

  state: {
    interviewing: '',
    next: '',
    info: {},
  },

  effects: {
    *fetchInfo({ payload }, { call, put }) {
      const { interviewing, next } = payload;
      const response = yield call(info, interviewing);
      yield put({
        type: 'save',
        payload: { 
          interviewing,
          next,
          info: response,
        },
      });
    },
    *fetch({ room }, { call, put }) {
      const response = yield call(ing, room);
      yield put({
        type: 'fetchInfo',
        payload: response,
      });
    },
    *comm({ payload, room }, { call }) {
      const response = yield call(comm, room, payload);
      message.info(`${response}`);
    },
    *shutdown(_, { call }) {
      const response = yield call(info);
      message.info(response);
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