[
    {
        "id": "$id_inject",
        "type": "inject",
        "z": "",
        "name": "timer",
        "props": [
        ],
        "repeat": "$repeat",
        "crontab": "",
        "once": false,
        "onceDelay": 1,
        "topic": "",
        "payload": "",
        "payloadType": "date",
        "x": 200,
        "y": 200,
        "wires": [
            [
                "$id_http_request"
            ]
        ]
    },
    {
        "id": "$id_http_request",
        "type": "http request",
        "z": "",
        "name": "",
        "method": "GET",
        "ret": "obj",
        "paytoqs": "ignore",
        "url": "$http_url",
        "tls": "",
        "persist": false,
        "proxy": "",
        "insecureHTTPParser": false,
        "authType": "",
        "senderr": false,
        "headers": [
            {
                "keyType": "Content-Type",
                "keyValue": "",
                "valueType": "application/json",
                "valueValue": ""
            }
        ],
        "x": 620,
        "y": 200,
        "wires": [
            [
                "$id_model_selector"
            ]
        ]
    },
    {
        "id": "$id_model_selector",
        "type": "supmodel",
        "z": "",
        "protocol": "rest",
        "models": [],
        "x": 850,
        "y": 200,
        "wires": [
            [
                "$id_mqtt"
            ]
        ]
    },
    {
        "id": "$id_mqtt",
        "type": "mqtt out",
        "z": "",
        "name": "",
        "topic": "",
        "qos": "",
        "retain": "",
        "respTopic": "",
        "contentType": "",
        "userProps": "",
        "correl": "",
        "expiry": "",
        "broker": "85bb67b2dbefe3ba",
        "x": 1050,
        "y": 200,
        "wires": []
    },
    {
        "id": "$id_catch",
        "type": "catch",
        "z": "",
        "name": "",
        "scope": [
            "$id_http_request"
        ],
        "uncaught": false,
        "x": 1250,
        "y": 200,
        "wires": [
            [
                "$id_catch_debug"
            ]
        ]
    },
    {
        "id": "$id_catch_debug",
        "type": "debug",
        "z": "",
        "name": "error catch debug",
        "active": true,
        "tosidebar": true,
        "console": false,
        "tostatus": false,
        "complete": "false",
        "statusVal": "",
        "statusType": "auto",
        "x": 1500,
        "y": 200,
        "wires": []
    },
    {
        "id": "85bb67b2dbefe3ba",
        "type": "mqtt-broker",
        "name": "",
        "broker": "emqx",
        "port": "1883",
        "clientid": "",
        "autoConnect": true,
        "usetls": false,
        "protocolVersion": "4",
        "keepalive": "60",
        "cleansession": true,
        "autoUnsubscribe": true,
        "birthTopic": "",
        "birthQos": "0",
        "birthRetain": "false",
        "birthPayload": "",
        "birthMsg": {},
        "closeTopic": "",
        "closeQos": "0",
        "closeRetain": "false",
        "closePayload": "",
        "closeMsg": {},
        "willTopic": "",
        "willQos": "0",
        "willRetain": "false",
        "willPayload": "",
        "willMsg": {},
        "userProps": "",
        "sessionExpiry": ""
    }
]