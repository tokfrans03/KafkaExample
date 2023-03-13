# Kafa

## Usage


1. extract kafka

    `cd kafka`

1. Start Zookeeper
    
    `bin/zookeeper-server-start.sh config/zookeeper.properties`

1. start kafka server

    `bin/kafka-server-start.sh config/server.properties`

1. Remove old topic

    `bin/kafka-topics.sh --delete --topic sshd-log-topic --bootstrap-server localhost:9092`

1. Create topic

    `bin/kafka-topics.sh --create --topic sshd-log-topic --bootstrap-server localhost:9092` 

1. build
    
    I had trouble building with maven so I used Intellij instead

1. Run class 

    `cd streams.example`

    `mvn exec:java -Dexec.mainClass=myapps.SSHD`

1. Start producer
    
    `bin/kafka-console-producer.sh --topic sshd-log-topic --bootstrap-server localhost:9092`

1. Check output

    `bin/kafka-console-consumer.sh --topic sshd-log-topic --from-beginning --bootstrap-server localhost:9092`

1. Now paste the content from `syslog.txt` into the producer terminal. JSON objects should be printed to standard out of the maven terminal like below 



```json
{
    "agent": {
        "id": "db01"
    },
    "process": {
        "name": "sshd",
        "pid": 18989
    },
    "@timestamp": "2023-04-06T10:33:08.000000Z",
    "host": {
        "port": 61088,
        "ip": "192.168.41.24"
    },
    "message": "Apr  6 10:33:08 db01 sshd[18989]: Received disconnect from 192.168.41.24 port 61088:11: disconnected by user",
    "event": {
        "reason": "disconnected by user",
        "created": "2023-04-06T10:33:08.000000Z",
        "type": "stop"
    }
}
{
    "agent": {
        "id": "db01"
    },
    "process": {
        "name": "sshd",
        "pid": 18989
    },
    "@timestamp": "2023-04-06T10:33:08.000000Z",
    "host": {
        "port": 61088,
        "ip": "192.168.41.24"
    },
    "message": "Apr  6 10:33:08 db01 sshd[18989]: Disconnected from 192.168.41.24 port 61088",
    "event": {
        "created": "2023-04-06T10:33:08.000000Z",
        "type": "stop"
    }
}
{
    "agent": {
        "id": "db01"
    },
    "process": {
        "name": "sshd",
        "pid": 18989
    },
    "@timestamp": "2023-04-06T10:33:08.000000Z",
    "host": {},
    "message": "Apr  6 10:33:08 db01 sshd[18989]: pam_unix(sshd:session): session closed for user dba01",
    "event": {
        "created": "2023-04-06T10:33:08.000000Z",
        "type": "stop",
        "user": "dba01"
    }
}
{
    "agent": {
        "id": "db01"
    },
    "process": {
        "name": "sshd",
        "pid": 21428
    },
    "@timestamp": "2023-04-06T10:33:12.000000Z",
    "host": {
        "port": 50760,
        "ip": "192.168.41.23"
    },
    "message": "Apr  6 10:33:12 db01 sshd[21428]: Accepted password for dba01 from 192.168.41.23 port 50760 ssh2",
    "event": {
        "created": "2023-04-06T10:33:12.000000Z",
        "type": "allowed",
        "user": "dba01"
    }
}
{
    "agent": {
        "id": "db01"
    },
    "process": {
        "name": "sshd",
        "pid": 21428
    },
    "@timestamp": "2023-04-06T10:33:12.000000Z",
    "host": {},
    "message": "Apr  6 10:33:12 db01 sshd[21428]: pam_unix(sshd:session): session opened for user dba01 by (uid=0)",
    "event": {
        "uid": 0,
        "created": "2023-04-06T10:33:12.000000Z",
        "type": "start",
        "user": "dba01"
    }
}
```