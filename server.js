var config = require('./config').config;
var io = require('socket.io')(3001);
var getInfo = require('./getinfo');
var pool = require('./pool');

io.on('connect', function(socket) {
    var room = -1;
    var intv = -1;

    socket.send('welc rjb int sys');

    socket.on('message', function(msg) {
        if (msg.slice(0,10) == 'i am inter') {
            socket.join('room' + Math.ceil(msg.slice(10)/3), function() {
                room = Math.ceil(msg.slice(10)/3);
                socket.to('dash').emit('room', 'online' + room);
                console.log(socket.rooms);
            });
            intv = msg.slice(10);
        }
        else if (msg == 'i am dashb') socket.join('dash', function() {
            room = 0;
            console.log(socket.rooms);
        })
        console.log(msg);
    });

    socket.on('disconnect', function(reason) {
        console.log('Disconnected:' + reason);
        if (room >= 1) socket.to('dash').emit('offl', room);
    });

    //interviewer event

    socket.on('grade', function(grade, cb) {
        console.log(grade);
        pool.setGrade(room, intv%3, grade.grade, grade.comment, function() {
            cb('提交成功');
            socket.to('dash').emit('roomnext', room);
        });
    });

    socket.on('nextone', function(room) {
        pool.nextOne(room, function(res) {
            socket.emit('')
        })
    });

    //universal event 

    socket.on('getinfo', function(id, cb) {
        console.log('Get info ' + id);
        getInfo.getInfo(id, function(res) {
            console.log(res);
            socket.emit('stuinfo', res);
        })
    });

    //dashboard event

    socket.on('inqueue', function(id) {
        console.log('start inqueue ' + id);
        pool.shortest(function(room) {
            pool.pushPool(id, function() {
                console.log('push into ' + room);
                socket.emit('inpool', id);
            }, room);
        });
    });

    socket.on('pushroom', function(msg) {
        getInfo.getInfo(msg.stuNo, function(res) {
            console.log('push' + msg.stuNo + ' into' + msg.room);
            pool.setRoom(msg.room, msg.stuNo, function() {
                socket.to('room' + msg.room).emit('new', res);
            });
        })
    })

});




