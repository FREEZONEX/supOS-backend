[
    {
        "id": "$id_inject",
        "type": "inject",
        "z": "",
        "name": "",
        "props": [

        ],
        "repeat": "$poll_rate",
        "crontab": "",
        "once": true,
        "onceDelay": 0.1,
        "topic": "",
        "x": 250,
        "y": 240,
        "wires": [
            [
                "$id_ui_ping"
            ]
        ]
    },
    {
        "id": "$id_ui_ping",
        "type": "ui ping",
        "z": "",
        "name": "",
        "host": "$ping_ip",
        "timeout": "$ping_timeout",
        "requests": "1",
        "x": 480,
        "y": 240,
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
        "protocol": "icmp",
        "models": [],
        "mappings": "",
        "x": 760,
        "y": 240,
        "wires": [
            [
                "$id_mqtt_out"
            ],
            []
        ]
    },
    {
        "id": "$id_mqtt_out",
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
        "x": 1030,
        "y": 240,
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