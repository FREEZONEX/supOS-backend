[
    {
        "id": "$id_modbus_getter",
        "type": "modbus-flex-getter",
        "z": "",
        "name": "",
        "showStatusActivities": false,
        "showErrors": true,
        "showWarnings": true,
        "logIOActivities": false,
        "server": "$id_modbus_client",
        "useIOFile": false,
        "ioFile": "",
        "useIOForPayload": false,
        "emptyMsgOnFail": false,
        "keepMsgProperties": true,
        "delayOnStart": false,
        "startDelayTime": "",
        "x": 670,
        "y": 100,
        "wires": [
            [
                "$id_model_selector"
            ],
            []
        ]
    },
    {
        "id": "$id_inject",
        "id_server": "$id_modbus_client",
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
        "payload": "$modbus_config_json_array",
        "payloadType": "json",
        "x": 230,
        "y": 100,
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
        "func": "let arr = msg.payload;\nfor (let i in arr) {\n    msg.payload=arr[i];\n    msg.model=arr[i].model;\n  node.send(msg);\n}\n",
        "outputs": 1,
        "timeout": 0,
        "noerr": 0,
        "initialize": "",
        "finalize": "",
        "libs": [],
        "x": 400,
        "y": 100,
        "wires": [
            [
                "$id_modbus_getter"
            ]
        ]
    },
    {
        "id": "$id_model_selector",
        "id_server": "$id_modbus_client",
        "type": "supmodel",
        "z": "",
        "protocol": "modbus",
        "models": [],
        "mappings": "",
        "x": 910,
        "y": 100,
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
        "x": 1230,
        "y": 100,
        "wires": []
    },
    {
        "id": "$id_modbus_client",
        "type": "modbus-client",
		"z":"",
        "name": "$modbus_client_name",
        "clienttype": "tcp",
        "bufferCommands": true,
        "stateLogEnabled": false,
        "queueLogEnabled": false,
        "failureLogEnabled": true,
        "tcpHost": "$modbus_host",
        "tcpPort": "$modbus_port",
        "tcpType": "DEFAULT",
        "serialPort": "/dev/ttyUSB",
        "serialType": "RTU-BUFFERD",
        "serialBaudrate": "9600",
        "serialDatabits": "8",
        "serialStopbits": "1",
        "serialParity": "none",
        "serialConnectionDelay": "100",
        "serialAsciiResponseStartDelimiter": "0x3A",
        "unit_id": "$modbus_unit_id",
        "commandDelay": "1",
        "clientTimeout": "1000",
        "reconnectOnTimeout": true,
        "reconnectTimeout": "2000",
        "parallelUnitIdsAllowed": true,
        "showErrors": false,
        "showWarnings": true,
        "showLogs": true
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