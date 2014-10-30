import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import org.apache.commons.codec.binary.Base64;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Main {

    Properties properties;

    public static void main(String[] args) {
        Main main = new Main();
        try {
            main.run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() throws InterruptedException {
        List<String> instanceIds = new ArrayList<>();
        try {
            properties = readProperties();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }

        AWSCredentials credentials = new BasicAWSCredentials(properties.getProperty("aws.access.key"),
                properties.getProperty("aws.secret.key"));
        AmazonEC2 ec2 = new AmazonEC2Client(credentials);
        ec2.setRegion(Region.getRegion(Regions.EU_WEST_1));

        try {
            instanceIds = vote(ec2);

            while (true) {
                long delayMinutes = (long) (Integer.parseInt(properties.getProperty("vote.minimum.delay")) +
                        (Math.random() * Integer.parseInt(properties.getProperty("vote.maximum.delay"))));
                log("Waiting for " + delayMinutes + " minutes before voting again");
                Thread.sleep(delayMinutes * 1000 * 60);
                log("Terminating " + instanceIds);
                ec2.terminateInstances(new TerminateInstancesRequest(instanceIds));
                instanceIds = vote(ec2);
            }
        } finally {
            ec2.terminateInstances(new TerminateInstancesRequest(instanceIds));
        }
    }

    private Properties readProperties() throws IOException {
        InputStream input = null;
        try {
            input = new FileInputStream("config.properties");
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<String> vote(AmazonEC2 ec2) throws InterruptedException {
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        runInstancesRequest.withImageId(properties.getProperty("aws.instance.ami"))
                .withInstanceType(properties.getProperty("aws.instance.type"))
                .withUserData(getUserDataScript(properties.getProperty("vote.user.data")))
                .withMinCount(1)
                .withMaxCount(1);

        RunInstancesResult requestResult = ec2.runInstances(runInstancesRequest);
        Reservation reservation = requestResult.getReservation();
        List<String> instanceIds = new ArrayList<>();
        for (Instance instance : reservation.getInstances()) {
            instanceIds.add(instance.getInstanceId());
        }
        log("Started instances with IDs " + instanceIds);
        return instanceIds;
    }

    private String getUserDataScript(String curlCommand){
        StringBuilder builder = new StringBuilder();
        builder.append("#! /bin/bash")
                .append("\n")
                .append(curlCommand.replaceAll("@USER_AGENT", RandomUserAgentGenerator.getRandomUserAgent()))
                .append("\n")
                .append("shutdown -h 0");
        log("USER DATA " + builder.toString());
        return new String(Base64.encodeBase64(builder.toString().getBytes()));
    }

    private static void log(String message) {
        System.out.println(new Date() + ": " + message);
    }
}
