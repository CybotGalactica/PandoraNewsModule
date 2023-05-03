const WebSocket = require('ws');
global.WebSocket = WebSocket;

const util = require("util")
global.TextEncoder = util.TextEncoder;
global.TextDecoder = util.TextDecoder

const ReconnectingWebSocket = require("reconnecting-websocket")
const { Client } = require("@stomp/stompjs")

const isTest = process.env.PROXY_IS_TEST
const destination_addr = process.env.PROXY_DEST_ADDR

const edition = "615c0331-6e61-4f56-8ea7-b2779ab0d89f";
const sourceUrl = isTest ? "ws://localhost:10000" : `wss://iapandora.nl/editions/${edition}/feed`


let queue = []


// Source Websocket connection

const ws = new ReconnectingWebSocket(sourceUrl, [], {WebSocket});
ws.onopen = () => {
    console.log(`Connected to source ${sourceUrl}`)
}
ws.onclose = () => {
    console.log("Closed connection to source")
}
ws.onmessage = (event) => queue.push(event.data)
ws.onerror = (event) => {
    console.error('[WebSocket error]', event.message);
};

// Destination STOMP connection

const stomp = new Client({
    brokerURL: `ws://${destination_addr}`,
    debug: console.log
})

stomp.onStompError = console.error

let sendIntervalID = null;
stomp.onConnect = () => {
    console.log("Connected to destination")
    sendIntervalID = setInterval(() => {
        if (queue.length > 0 && stomp.active) {
            const message = queue
            queue = []
            try {
                stomp.publish({
                    destination: "/",
                    body: `[${message.join(',')}]`,
                })
            } catch (e) {
                console.error(e)
                queue = message.concat(queue)
            }
        }
    }, 500)
}

stomp.onWebSocketClose = () => {
    console.log("connection closed")
    if (sendIntervalID !== null) {
        clearInterval(sendIntervalID)
    }
}

stomp.activate()

// Test messages sent to WS echo server

if (isTest) {
    console.log("starting test messages")
    setInterval(() => {
        ws.send("{" +
                "    \"type\": \"solve\"," +
                "    \"timestamp\": \"2023-05-03T18:26:40.058182+00:00\"," +
                "    \"message\": \"Test solved bonus puzzle 1.\"," +
                "    \"solve\": {" +
                "        \"team\": {" +
                "            \"id\": \"6d8304e8-e169-4600-92a9-06165131404e\"," +
                "            \"name\": \"Test\"" +
                "        }," +
                "        \"code\": {" +
                "            \"day\": null," +
                "            \"number\": 1" +
                "        }" +
                "    }" +
                "}")
    }, 5000)
    setInterval(() => {
        ws.send("{" +
                "    \"type\": \"solve\"," +
                "    \"timestamp\": \"2023-05-03T18:29:35.970522+00:00\"," +
                "    \"message\": \"Test solved puzzle 2 of day 1.\"," +
                "    \"solve\": {" +
                "        \"team\": {" +
                "            \"id\": \"6d8304e8-e169-4600-92a9-06165131404e\"," +
                "            \"name\": \"Test\"" +
                "        }," +
                "        \"code\": {" +
                "            \"day\": {" +
                "                \"number\": 1" +
                "            }," +
                "            \"number\": 2" +
                "        }" +
                "    }" +
                "}")
    }, 2777)
    setInterval(() => {
        ws.send("{" +
                "    \"type\": \"kill\"," +
                "    \"timestamp\": \"2023-05-03T18:28:26.068169+00:00\"," +
                "    \"message\": \"Daniëlle (Test) killed Henkie de Panda (Test 2).\"," +
                "    \"kill\": {" +
                "        \"killer\": {" +
                "            \"id\": \"923fff4a-534e-42d0-afb4-4ab680cd50b1\"," +
                "            \"name\": \"Daniëlle\"," +
                "            \"team\": {" +
                "                \"id\": \"6d8304e8-e169-4600-92a9-06165131404e\"," +
                "                \"name\": \"Test\"" +
                "            }" +
                "        }," +
                "        \"victim\": {" +
                "            \"id\": \"68265dab-e121-46a8-bd43-17d2f562754b\"," +
                "            \"name\": \"Henkie de Panda\"," +
                "            \"team\": {" +
                "                \"id\": \"59dbb829-705b-439d-a21a-62eb6266cb67\"," +
                "                \"name\": \"Test 2\"" +
                "            }" +
                "        }" +
                "    }" +
                "}")
    }, 3567)
}
