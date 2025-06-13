[

    {
        "id": "$id_opcua_client",
        "type": "OpcUa-Client",
        "z": "",
        "endpoint": "$id_opcua_server",
        "action": "subscribe",
        "deadbandtype": "a",
        "deadbandvalue": 1,
        "time": $pollRate,
        "timeUnit": "$pollRateUnit",
        "certificate": "n",
        "localfile": "",
        "localkeyfile": "",
        "securitymode": "None",
        "securitypolicy": "None",
        "useTransport": false,
        "maxChunkCount": 1,
        "maxMessageSize": 8192,
        "receiveBufferSize": 8192,
        "sendBufferSize": 8192,
        "name": "",
        "x": 640,
        "y": 160,
        "wires": [
            [
                "$id_model_selector"
            ],
            [
                "$id_model_selector"
            ],
            []
        ]
    },
    {
        "id": "$id_inject",
        "id_server": "$id_opcua_server",
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
        "repeat": "",
        "crontab": "",
        "once": true,
        "onceDelay": 1,
        "topic": "multiple",
        "payload": "$payload_array_string",
        "payloadType": "json",
        "x": 210,
        "y": 160,
        "wires": [
            [
                "$id_opcua_client"
            ]
        ]
    },
    {
        "id": "$id_model_selector",
        "id_server": "$id_opcua_server",
        "type": "supmodel",
        "z": "",
        "protocol": "opcua",
        "models": [],
        "mappings": "",
        "x": 870,
        "y": 160,
        "wires": [
            [
                "$id_mqtt_out"
            ],
            [

            ]
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
        "x": 1270,
        "y": 160,
        "wires": []
    },
    {
        "id": "$id_opcua_server",
        "type": "OpcUa-Endpoint",
        "endpoint": "$opcua_server_addr",
        "secpol": "None",
        "secmode": "None",
        "none": true,
        "login": false,
        "usercert": false,
        "usercertificate": "",
        "userprivatekey": ""
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