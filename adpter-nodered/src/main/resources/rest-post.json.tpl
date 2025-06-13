[

    {
        "id": "$id_http_request",
        "type": "http request",
        "z": "",
        "name": "",
        "method": "POST",
        "ret": "txt",
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
                "keyType": "Accept",
                "keyValue": "",
                "valueType": "application/json",
                "valueValue": ""
            },
            {
                "keyType": "Content-Type",
                "keyValue": "",
                "valueType": "application/json",
                "valueValue": ""
            }
        ],
        "x": 660,
        "y": 280,
        "wires": [
            [
                "$id_model_selector"
            ]
        ]
    },
    {
        "id": "$id_inject",
        "type": "inject",
        "z": "",
        "name": "",
        "props": [
            {
                "p": "payload"
            },
            {
                "p": "topic",
                "vt": "str"
            }
        ],
        "repeat": "$repeat",
        "crontab": "",
        "once": false,
        "onceDelay": 0.1,
        "topic": "",
        "payload": "",
        "payloadType": "date",
        "x": 270,
        "y": 280,
        "wires": [
            [
                "$id_func"
            ]
        ]
    },
    {
        "id": "$id_func",
        "type": "function",
        "z": "",
        "name": "request body",
        "func": "msg.payload=$http_body\nreturn msg;",
        "outputs": 1,
        "timeout": 0,
        "noerr": 0,
        "initialize": "",
        "finalize": "",
        "libs": [],
        "x": 460,
        "y": 280,
        "wires": [
            [
                "$id_http_request"
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