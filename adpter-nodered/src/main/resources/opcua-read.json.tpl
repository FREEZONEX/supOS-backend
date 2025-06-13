[

    {
        "id": "$id_opcua_item",
        "type": "OpcUa-Item",
        "z": "",
        "item": "$item_addr",
        "datatype": "$data_type",
        "value": "",
        "name": "",
        "x": 400,
        "y": 80,
        "wires": [
            [
                "$id_opcua_client"
            ]
        ]
    },
    {
        "id": "$id_opcua_client",
        "type": "OpcUa-Client",
        "z": "",
        "endpoint": "$id_opcua_server",
        "action": "read",
        "deadbandtype": "a",
        "deadbandvalue": 1,
        "time": 10,
        "timeUnit": "s",
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
        "x": 680,
        "y": 80,
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
        "id": "$id_inject",
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
        "onceDelay": 0.1,
        "topic": "",
        "payload": "",
        "payloadType": "date",
        "x": 180,
        "y": 80,
        "wires": [
            [
                "$id_opcua_item"
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
        "x": 940,
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
        "id": "$id_opcua_server",
        "type": "OpcUa-Endpoint",
        "z": "",
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