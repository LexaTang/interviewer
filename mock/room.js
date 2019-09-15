
export default {
  
  'GET /api/room/1': {"interviewing":"1500720134","next":"1500720135"},
  'GET /api/room/2': {"interviewing":"1500720136","next":"1500720137"},

  'PUT /api/room/1': {"res": "cached"},

  'GET /api/get': {"queue": ["1900100101", "1900100102"], "vip":[{"1900300101": 2}, {"1900300102": 1}]},

  // 支持自定义函数，API 参考 express@4
  'POST /api/users/create': (req, res) => {
    res.end('OK');
  },
};