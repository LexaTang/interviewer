var redis = require('redis');
var assert = require('assert');
const config = require('./config').config;
var client = redis.createClient();
var getInfo = require('./getinfo');

client.on("error", (err) => console.log("Redis error:" + err));

for (i = 1; i <= 5; i++) {
    client.HMSET('groom' + i, 0, -1, 1, -1, 2, -1);
    client.LTRIM('croom' + i, -1, 0);
}

function shutdown() {
    client.end(true);
}

function shortest(cb) {
    var lens = new Array(6);
    for (i = 0; i < 6; i++) {
        lens[i] = Number.MAX_VALUE;
    }
    config.enabledrooms.forEach( function(i) {
        lens[i] = -1;
    });
    
    config.enabledrooms.forEach( function(i) {
        var ig = i;
        client.LLEN('room' + i, function(_, ind) {
            lens[ig] = ind;
            if (lens.indexOf(-1) == -1) cb(lens.indexOf(Math.min.apply(Math.min, lens)));
        });
    });
}

function pushPool(id, cb, room) {
    room = room || 0;

    if (room != 0)
        client.RPUSH("room0", id);
    return client.RPUSH("room" + room, id, cb);
}

function getPool(ind, cb, room) {
    room = room || 0;
    return client.LINDEX("room" + room, ind, cb);
}

function popPool(cb, room) {
    room = room || 0;

    if (room != 0) {
        return client.LPOP("room" + room, (_, id) => {
            client.LPOP("room0", () => {
                client.SET('inroom' + room, id, () => {
                cb(null, id);
                })
            })
        });
    } 
    return client.LPOP("room0", cb);
} 

function isFin(room, cb) {
    client.HMGET('groom' + room, 0, 1, 2, function(_, strAry) {
        console.log(strAry);
        console.log(strAry.indexOf('-1'));
        console.log(strAry.indexOf('-1') == -1);
        cb(strAry.indexOf('-1') == -1);
    });
}

function ssetGrade(room, intv, grade, comment, cb) {
    client.HSET('groom' + room, intv, grade, () => {
        client.RPUSH('croom' + room, comment, function () {
            isFin(room, function (fin) {
                console.log('is fin ' + fin);
                if (fin) client.GET('inroom' + room, function (_, id) {
                    client.HMGET('groom' + room, 0, 1, 2, function (_, strAry) {
                        client.LPOP('croom' + room, function (_, comm) {
                            client.LTRIM('croom' + room, -1, 0);
                            client.HMSET('groom' + room, 0, -1, 1, -1, 2, -1);
                            getInfo.pushGrade(id, strAry, comm, room, cb);
                        });
                    });
                });
                else console.log('new grade in ' + room + 'intv ' + intv);
            });
        });
    });
}

function setRoom(room, id, cb) {
    client.SET('inroom' + room, id, cb);
}

function setGrade(room, intv, grade, comment, cb) {
    client.HSET('groom' + room, intv, grade, () => {
        client.RPUSH('croom' + room, comment, function () {
            isFin(room, function (fin) {
                console.log('is fin ' + fin);
                if (fin) client.GET('inroom' + room, function (_, id) {
                    client.HMGET('groom' + room, 0, 1, 2, function (_, strAry) {
                        client.LPOP('croom' + room, function(_, comm) {
                            getInfo.pushGrade(id, strAry, comm, room, cb);
                        });
                    });
                });
                else console.log('new grade in ' + room + 'intv ' + intv);
            });
        });
    });

}

function nextOne(room, cb) {
    client.LTRIM('croom' + room, -1, 0);
    client.HMSET('groom' + room, 0, -1, 1, -1, 2, -1);
    popPool(function (id) {
        getInfo.getInfo(id, function(res) {
            cb(res);
        });
    }, room);
}

module.exports = {
    pushPool: pushPool,
    getPool: getPool,
    popPool: popPool,
    nextOne: nextOne,
    setRoom: setRoom,
    setGrade: ssetGrade,
    shutdown: shutdown,
    shortest: shortest,
}