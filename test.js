var assert = require('assert');
require("should");
var config = require('./config').config;

describe('Requirement', () => {
    it('PostgreSQL', () => require('pg'));
    it('Redis', () => require('redis'));
    it('Socket.io', () => require('socket.io'));
});

describe('Config', () => {
    it('Properties',() => config.should.have.properties("intvnum", "enabledrooms", "remoteaddress", "socketport"));
    it('Number propertie',() => assert.strictEqual(config.socketport, +config.socketport));
})

describe('Pool', () => {
    var pool = require('./pool');
    it('Ensure all room empty', (done) => { 
        var i = 0;
        function recall(_, id) {
            assert.equal(id, null);
            if (i >= Math.max.apply(Math, config.enabledrooms)) {
                done();
                return;
            }
            i++;
            pool.getPool(i, recall);
        }

        recall();
    });

    it('Push into main pool', (done) => 
        pool.pushPool(1500720134, (_, ind) => {
            assert.notEqual(ind, 0);
            done();
    }));

    it('Get the first index', (done) => 
        pool.getPool(0, (_, id) => {
            assert.equal(id, 1500720134);
            done();
    }));
    
    it('Pop the main pool', (done) => 
        pool.popPool((_, id) => {
            assert.equal(id, 1500720134);
            done();
    }));

    it('Push into room1', (done) => 
        pool.pushPool(1500720134, (_, ind) => {
            assert.notEqual(ind, 0);
            done();
    }, 1));

    it('Get the room1', (done) => 
        pool.getPool(0, (_, id) => {
            assert.equal(id, 1500720134);
            done();
    }), 1);

    it('Pop the room1', (done) => 
        pool.popPool((_, id) => {
            assert.equal(id, 1500720134);
            pool.shutdown();
            done();
    }, 1));
    
});