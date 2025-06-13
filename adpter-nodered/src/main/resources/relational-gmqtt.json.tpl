[
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
        "repeat": "10",
        "crontab": "",
        "once": false,
        "onceDelay": 1,
        "topic": "",
        "payload": "",
        "payloadType": "date",
        "x": 300,
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
        "d": $disabled
        "z": "",
        "name": "mock data",
        "func": "// 随机字符串\nfunction randomString(length) {\n    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';\n    return Array.from({ length }, () => characters[Math.floor(Math.random() * characters.length)]).join('');\n}\n// 100以内随机数字\nfunction generateRandomNumber() {\n    return Math.floor(Math.random() * 100);\n}\n// 随机生成100以内浮点数，保留2位小数\nfunction generateRandomFloatWithTwoDecimals() {\n    const randomFloat = Math.random() * 100;\n    return randomFloat.toFixed(2);\n}\n// 对当前时间格式化\nfunction formatCurDate() {return Date.now();\n}\n\nfunction getBool() {\n    var randomInt = generateRandomNumber();\n    return randomInt > 50;\n}\nmsg.topic='4174348a-9222-4e81-b33e-5d72d2fd7f1e';\nmsg.payload = [\n{\n'alias':'$model_alias',\n'data': {$payload \n}}];\n\nreturn msg;",
        "outputs": 1,
        "timeout": 0,
        "noerr": 2,
        "initialize": "",
        "finalize": "",
        "libs": [],
        "x": 500,
        "y": 100,
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
        "x": 770,
		"y": 100,
        "wires": []
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