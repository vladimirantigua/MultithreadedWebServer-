webSocket = new WebSocket("ws://localhost:8080");

let username;
let message;

webSocket.onopen = function (event) {
    webSocket.send("get rooms");
    console.log("connection successful ");
};

webSocket.onclose = () => {
    console.log("disconnected")
};

webSocket.onerror = (error) => {
    console.log("failed to connect", error);
};

webSocket.onmessage = function (event) {
    console.log("new message");

    if (username == null) {
        let newMessage = document.createElement("li");
        newMessage.innerText = event.data;
        document.getElementById("availableRooms").append(newMessage);
    } else {
        let nextMessage = document.createElement("div");
        nextMessage.innerText = event.data;
        document.querySelector('#chat').append(nextMessage);

        var messageBody = document.querySelector('#chat');
        messageBody.scrollTop = messageBody.scrollHeight - messageBody.clientHeight;

        console.log ("USERNAME" + username);
    }
}

// To Send the message:

document.querySelector('form').addEventListener('submit', (event) => {

    event.preventDefault();
    console.log("Submit the message is working ");

    let message = document.querySelector('#message').value;
    document.querySelector('#message').value = '';
    console.log("This is the message that was sent: " + username + " " + message);
    webSocket.send(username + ": " + message);

});

// Enable the Submit button
document.querySelector('form').addEventListener('keydown', function (event) {
    if (event.code === 'Enter') {
        event.preventDefault();
        document.getElementById('submit1').click();

    }
});

// To Enter Room:
document.querySelector('#enterRoom').addEventListener('click', (event) => {
    console.log("room entered");

    // $('form.ghost').removeAttr('class');
    // $('div.ghost').removeAttr('class');

    username = document.querySelector('#userName').value;
    roomName = document.querySelector('#roomName').value;

    console.log("Username " + username);
    console.log("Roomname " + roomName);

    webSocket.send("join " + roomName);
    document.getElementById("initialSetup").innerText = "";

    let room = document.createElement("h3");
    let user = document.createElement("h3");
    room.innerText = "Room: " + roomName;
    user.innerText = "User: " + username;

    document.getElementById("chatHeader").append(room);
    document.getElementById("chatHeader").append(user);

});

// press enter  to enter a room:

document.querySelector('#roomName').addEventListener('keydown', function (event) {
    if (event.code === 'Enter') {
        event.preventDefault();
        document.getElementById('enterRoom').click();
    }
});