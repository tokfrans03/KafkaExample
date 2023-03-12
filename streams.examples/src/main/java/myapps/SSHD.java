package myapps;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

public class SSHD {

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "sshd-log-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("sshd-log-topic"));
        //System.out.println("Subscribed to topic sshd-log-topic");

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                String logMessage = record.value();
                //System.out.println("AAAAAAAAAAAAAAAAAAAAAAA");
                //System.out.println(logMessage);

                JSONObject jsonLog = parseLogMessage(logMessage);
                System.out.println(jsonLog.toString());
                //System.out.println("BBBBBBBBBBBBBBBBBBBBBBB");
                // System.out.printf("offset = %d, key = %s, value = %s\n",
                //         record.offset(), record.key(), record.value());

            }
        }
    }


    private static final Pattern LOG_PATTERN =
        Pattern.compile("(?<month>\\w{3})\\s+(?<day>\\d{1,2})\\s(?<time>[\\d:]+)\\s(?<host>\\S+)\\s+(?<process>\\S+)\\[(?<pid>\\d+)\\]:\\s+(?<message>.+)");

    public static JSONObject parseLogMessage(String message) {
        JSONObject jsonLog = new JSONObject();
        Matcher matcher = LOG_PATTERN.matcher(message);
        if (!matcher.matches()) {
            // The log message did not match the expected pattern.
            return null;
        }
        jsonLog.put("message", message);

        // Since no year is present in the log just put the current one
        int year = LocalDateTime.now().get(ChronoField.YEAR);
        String timestampString = String.format("%s %s %d %s", matcher.group("month"), matcher.group("day"), year, matcher.group("time"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d yyyy HH:mm:ss");
        DateTimeFormatter newFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");
        LocalDateTime timestamp = LocalDateTime.parse(timestampString, formatter);
        String TZ_timestamp = timestamp.atOffset(ZoneOffset.UTC).format(newFormat);

        // @timestamp
        jsonLog.put("@timestamp", TZ_timestamp);

        // Agent
        JSONObject agentObj = new JSONObject();
        agentObj.put("id", matcher.group("host"));


        // HOST
        JSONObject hostObj = new JSONObject();
        //hostObj.put("ip", matcher.group("host"));

        // EVENT
        JSONObject eventObj = new JSONObject();
        eventObj.put("created", TZ_timestamp);
        eventObj.put("type", "unknown");

        // process
        JSONObject processObj = new JSONObject();
        processObj.put("pid", Integer.parseInt(matcher.group("pid")));
        processObj.put("name", matcher.group("process"));


        String logMessage = matcher.group("message");
        // Parse the different types of messages from the message
        if (logMessage.startsWith("Received disconnect")) {
            // Received disconnect from 192.168.41.24 port 61088:11: disconnected by user
            eventObj.put("type", "stop");
            String[] parts2 = logMessage.split(":");
            eventObj.put("reason", parts2[parts2.length - 1].trim());
            String[] parts3 = parts2[0].split(" ");
            //jsonLog.put("user", parts3[3]);
            hostObj.put("ip", parts3[3]);
            hostObj.put("port", Integer.parseInt(parts3[5]));

        } else if (logMessage.startsWith("Disconnected from")) {
            // Disconnected from 192.168.41.24 port 61088
            eventObj.put("type", "stop");
            String[] parts2 = logMessage.split(" ");
            hostObj.put("ip", parts2[2]);
            hostObj.put("port", Integer.parseInt(parts2[4]));
        } else if (logMessage.startsWith("pam_unix(sshd:session): session closed")) {
            // pam_unix(sshd:session): session closed for user dba01
            eventObj.put("type", "stop");
            String[] parts2 = logMessage.split(" ");
            eventObj.put("user", parts2[5]);
        } else if (logMessage.startsWith("Accepted")) {
            // Accepted password for dba01 from 192.168.41.23 port 50760 ssh2
            eventObj.put("type", "allowed");
            String[] parts2 = logMessage.split(" ");
            eventObj.put("user", parts2[3]);
            hostObj.put("ip", parts2[5]);
            hostObj.put("port", Integer.parseInt(parts2[7]));
        } else if (logMessage.startsWith("pam_unix(sshd:session): session opened")) {
            // pam_unix(sshd:session): session opened for user dba01 by (uid=0)
            eventObj.put("type", "start");
            String[] parts2 = logMessage.split(" ");
            eventObj.put("user", parts2[5]);
            eventObj.put("uid", Integer.parseInt(parts2[7].replace("(uid=", "").replace(")", "")));
        }

        // Put all objects into final JSON
        jsonLog.put("agent", agentObj);
        jsonLog.put("host", hostObj);
        jsonLog.put("event", eventObj);
        jsonLog.put("process", processObj);

        return jsonLog;
    }
}

