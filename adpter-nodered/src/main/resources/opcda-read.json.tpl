[
    {
        "id": "$id_opcda_read",
        "type": "opcda-read",
        "z": "",
        "server": "$id_opcda_server",
        "name": "",
        "updaterate": 1000,
        "cache": false,
        "datachange": false,
        "groupitems": [],
        "x": 490,
        "y": 260,
        "wires": [
            [
                "$id_model_selector"
            ]
        ]
    },
    {
        "id": "$id_inject",
        "id_server": "$id_opcda_server",
        "type": "inject",
        "z": "",
        "name": "",
        "props": [
            {
                "p": "payload"
            }
        ],
        "repeat": "$pollRate",
        "crontab": "",
        "once": false,
        "onceDelay": 1,
        "topic": "",
        "x": 240,
        "y": 260,
        "wires": [
            [
                "$id_opcda_read"
            ]
        ]
    },
    {
        "id": "$id_model_selector",
        "id_server": "$id_opcda_server",
        "type": "supmodel",
        "z": "",
        "protocol": "opcda",
        "models": [],
        "mappings": "",
        "x": 710,
        "y": 260,
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
        "x": 970,
        "y": 260,
        "wires": []
    },
    {
        "id": "$id_opcda_server",
        "type": "opcda-server",
        "name": "",
        "address": "$opcda_server_host",
        "domain": "$opcda_server_domain",
        "clsid": "$opcda_server_clsid",
        "timeout": $opcda_server_timeout,
        "credentials": {"username": "$opcda_server_account","password": "$opcda_server_password"}
    },
    {
        "id": "85bb67b2dbefe3ba",
        "type": "mqtt-broker",
        "z": "",
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