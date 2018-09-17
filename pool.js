var redis = require('redis');
var assert = require('assert');
var client = redis.createClient();


client.on("error", (err) => console.log("Redis error:" + err));

function shutdown() {
    client.end(true);
}

function pushPool(id, cb, room) {
    room = room || 0;

    if (room != 0)
        client.LPUSH("room0", id);
    return client.LPUSH("room" + room, id, cb);
}

function getPool(ind, cb, room) {
    room = room || 0;
    return client.LINDEX("room" + room, ind, cb);
}

function popPool(cb, room) {
    room = room || 0;

    if (room != 0) {
        return client.RPOP("room" + room, (_, id) => {
            client.RPOP("room0", (_, id2) => {
                assert.equal(id, id2);
                cb(null, id);
            })
        });
    } 
    return client.RPOP("room0", cb);
} 

module.exports = {
    pushPool: pushPool,
    getPool: getPool,
    popPool: popPool,
    shutdown: shutdown,
}