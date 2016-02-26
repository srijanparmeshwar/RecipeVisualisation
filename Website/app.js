var fs = require('fs');
var http = require('http');
var express = require("express");
var bodyParser = require("body-parser");
var app = express();

app.use(bodyParser.text({type: 'text/html'}));
app.use(bodyParser.urlencoded({ extended: false }))
app.use(bodyParser.json())

app.get("/", function (req, res) {
    console.log('File request: ' + "index");
    res.sendFile(__dirname + "/index.html");
});

app.get("/api/search", function (req, res) {
    console.log('Query request: ' + req.query.q);
    var options = {
        host: 'localhost',
        path: '/search?q=' + encodeURIComponent(req.query.q),
        port: '4567'
    };

    function handler(response) {
        var result = '';

        response.on('data', function (chunk) {
            result += chunk;
        });

        response.on('end', function () {
            if(response.statusCode != 200) res.status(400);
            res.send(result);
        });
    };

    console.log('Forwarding to Java server: ' + req.query.q);
    var request = http.request(options, handler);
    request.on("error", function() {
        console.log("Error occurred processing: " + req.query.q);
        res.status(400);
        res.send("");
    });
    request.end();
});

app.get("/api/recipes/:id", function (req, res) {
    console.log('Query request: ' + req.params.id);
    var options = {
        host: 'localhost',
        path: '/recipes/' + encodeURIComponent(req.params.id),
        port: '4567'
    };

    function handler(response) {
        var result = '';

        response.on('data', function (chunk) {
            result += chunk;
        });

        response.on('end', function () {
            if(response.statusCode != 200) res.status(400);
            res.send(result);
        });
    };

    console.log('Forwarding to Java server: ' + req.params.id);
    var request = http.request(options, handler);
    request.on("error", function() {
        console.log("Error occurred processing: " + req.params.id);
        res.status(400);
        res.send("");
    });
    request.end();
});

app.post("/api/upload", function (req, res) {
    console.log('Upload request.');
    var options = {
        host: 'localhost',
        path: '/upload',
        port: '4567',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    function handler(response) {
        var result = '';

        response.on('data', function (chunk) {
            result += chunk;
        });

        response.on('end', function () {
            if(response.statusCode != 200) res.status(400);
            res.send(result);
        });
    };

    console.log('Forwarding to Java server.');
    var request = http.request(options, handler);
    request.on("error", function() {
        console.log("Error occurred processing upload.");
        res.status(400);
        res.send("");
    });

    request.write(JSON.stringify(req.body));
    request.end();
});

/* Serves all the static files */
app.get(/^(.+)$/, function (req, res) {
    var options = {};
    console.log('File request: ' + req.params[0]);
    res.sendFile(__dirname + req.params[0], options, function (err) {
        if (err) {
            console.log("Error: " + err.status);
            res.status(404);
            res.send("File not found");
        }
    });
});

var httpServer = http.createServer(app);

httpServer.listen(80, function () {
    console.log("Listening on port 80");
});
