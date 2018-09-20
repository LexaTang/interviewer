var redis = require('redis');
var assert = require('assert');
const config = require('./config').config;
var client = redis.createClient();


client.on("error", (err) => console.log("Redis error:" + err));

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
            client.LPOP("room0", (_, id2) => {
                assert.equal(id, id2);
                cb(null, id);
            })
        });
    } 
    return client.LPOP("room0", cb);
} 

module.exports = {
    pushPool: pushPool,
    getPool: getPool,
    popPool: popPool,
    shutdown: shutdown,
    shortest: shortest,
}