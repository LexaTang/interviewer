var config = require('./config').config;
var io = require('socket.io')(3001);

io.on('connect', function(socket) {
    var room = -1;

    socket.send('welc rjb int sys');

    socket.on('message', function(msg) {
        if (msg.slice(0,10) == 'i am inter') socket.join('room' + Math.ceil(msg.slice(10)/3), function() {
            room = Math.ceil(msg.slice(10)/3);
            socket.to('dash').emit('room', 'online' + room);
            console.log(socket.rooms);
        });
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

    socket.on('grade', function(grade, cb) {
        console.log(grade);
        cb('提交成功');
    });
});




