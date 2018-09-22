const { Pool } = require('pg');
var redis = require('redis');
const config = require('./config').config;
var client = redis.createClient();
const pool = new Pool({
  user: 'postgres',
  host: 'localhost',
  database: 'intv',
  password: 'tcjpxh',
  port: 5432,
});

client.on("error", (err) => console.log("Redis error:" + err));

function getInfo(id, cb) {
    pool.query('SELECT * FROM public.student WHERE stnum=$1;', [id]).then(function(res) {
        cb(res.rows[0]);
    });
}

function pushGrade(id, gradeAry, comment, room, cb) {
    pool.query('INSERT INTO public.grades VALUES ($1, $2, $3, $4);', [id, gradeAry, comment.toString(), room]).then(res => {
        console.log('push grade database');
        if (cb) cb(res);
    })
}

module.exports = {
    getInfo: getInfo,
    pushGrade: pushGrade,
}