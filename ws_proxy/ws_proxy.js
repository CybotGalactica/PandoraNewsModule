Object.assign(global, { WebSocket: require('ws') });
const util = require("util")
global.TextEncoder = util.TextEncoder;
global.TextDecoder = util.TextDecoder
const ReconnectingWebSocket = require("reconnecting-websocket")
const { Client } = require("@stomp/stompjs")

const isTest = process.env.PROXY_IS_TEST
const destination_addr = process.env.PROXY_DEST_ADDR

const sourceUrl = isTest ? "ws://localhost:10000" : "wss://iapandora.nl/ws/pandora"

let queue = []


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

if (isTest) {
    console.log("starting test messages")
    setInterval(() => {
        ws.send("{\n" +
            "    \"type\": \"puzzle\",\n" +
            "    \"team\": {\n" +
            "      \"id\": \"Beagle Boys\",\n" +
            "      \"name\": \"Beagle Boys\"\n" +
            "    },\n" +
            "    \"puzzle\": {\n" +
            "      \"number\": 1,\n" +
            "      \"title\": \"Climbing trees with Prof. Banana\",\n" +
            "      \"bonus\": false\n" +
            "    },\n" +
            "    \"time_bonus\": 0,\n" +
            "    \"message\": \"Beagle Boys solved puzzle 1.\"\n" +
            "  }")
    }, 5000)
    setInterval(() => {
        ws.send("{\n" +
            "    \"type\": \"kill\",\n" +
            "    \"killer\": {\n" +
            "      \"id\": 15,\n" +
            "      \"name\": \"Knaboss\",\n" +
            "      \"team\": {\n" +
            "        \"id\": 14,\n" +
            "        \"name\": \"Knaboeven\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"victim\": {\n" +
            "      \"id\": 72,\n" +
            "      \"name\": \"Obsidian\",\n" +
            "      \"team\": {\n" +
            "        \"id\": 12,\n" +
            "        \"name\": \"Diamond hoes\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"message\": \"Knaboss pwned Obsidian's head!\"\n" +
            "  }")
    }, 2777)
}
