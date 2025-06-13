[
    {
        "id": "$id_inject",
        "id_server": "$id_custom_protocol_server",
        "type": "inject",
        "z": "",
        "name": "",
        "props": [
            {
                "p": "payload"
            }
        ],
        "repeat": "10",
        "crontab": "",
        "once": false,
        "onceDelay": 0.1,
        "topic": "",
        "payload": "$payload_json_array",
        "payloadType": "json",
        "x": 210,
        "y": 80,
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
        "name": "Loop",
        "func": "let arr = msg.payload;\nfor (let i in arr) {\n    msg.model=arr[i].model;\n  node.send(msg);\n}\n",
        "outputs": 1,
        "timeout": 0,
        "noerr": 0,
        "initialize": "",
        "finalize": "",
        "libs": [],
        "x": 400,
        "y": 80,
        "wires": [
            [
                "$id_custom_protocol_node"
            ]
        ]
    },
    {
        "id": "$id_model_selector",
        "id_server": "$id_custom_protocol_server",
        "type": "supmodel",
        "z": "",
        "protocol": "",
        "models": [],
        "mappings": "",
        "x": 870,
        "y": 80,
        "wires": [
            [
                "$id_mqtt"
            ],
            [

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
        "x": 1270,
        "y": 80,
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