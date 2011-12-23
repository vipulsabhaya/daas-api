package com.hpcloud.daas.ec2;

/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AssociateAddressResult;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.ec2.model.Volume;

/**
 * Welcome to your new AWS Java SDK based project!
 *
 * This class is meant as a starting point for your console-based application
 * that makes one or more calls to the AWS services supported by the Java SDK,
 * such as EC2, SimpleDB, and S3.
 *
 * In order to use the services in this sample, you need:
 *
 * - A valid Amazon Web Services account. You can register for AWS at:
 * https://aws-portal.amazon.com/gp/aws/developer/registration/index.html
 *
 * - Your account's Access Key ID and Secret Access Key:
 * http://aws.amazon.com/security-credentials
 *
 * - A subscription to Amazon EC2. You can sign up for EC2 at:
 * http://aws.amazon.com/ec2/
 *
 * - A subscription to Amazon SimpleDB. You can sign up for Simple DB at:
 * http://aws.amazon.com/simpledb/
 *
 * - A subscription to Amazon S3. You can sign up for S3 at:
 * http://aws.amazon.com/s3/
 */
public class AwsConsoleApp
{

    /*
     * Important: Be sure to fill in your AWS access credentials in the
     * AwsCredentials.properties file before you try to run this sample.
     * http://aws.amazon.com/security-credentials
     */

    static AmazonEC2 ec2;

    /**
     * The only information needed to create a client are security credentials
     * consisting of the AWS Access Key ID and Secret Access Key. All other
     * configuration, such as the service endpoints, are performed
     * automatically. Client parameters, such as proxies, can be specified in an
     * optional ClientConfiguration object when constructing a client.
     *
     * @see com.amazonaws.auth.BasicAWSCredentials
     * @see com.amazonaws.auth.PropertiesCredentials
     * @see com.amazonaws.ClientConfiguration
     */
    private static void init() throws Exception
    {
        try
        {
            AWSCredentials credentials = new PropertiesCredentials(AwsConsoleApp.class.getResourceAsStream("AwsCredentials.properties"));

            ec2 = new AmazonEC2Client(credentials);
            // set our endpoint to HP Cloud
            ec2.setEndpoint("https://az-2.region-a.geo-1.ec2-compute.hpcloudsvc.com/services/Cloud/");

        } catch (Exception e)
        {
            System.out.println(e);
        }
    }

    public static void main(String[] args) throws Exception
    {
//        System.out.println("===========================================");
//        System.out.println("Welcome to the AWS Java SDK!");
//        System.out.println("===========================================");
//
        init();
//
//        describeImages();
//        //
//        List<String> securityGroups = new ArrayList<String>();
//        securityGroups.add("default");
//
//        describeInstances();
//
//        printAllSecurityGroups();
//
//        describeKeyPairs();
//
//        describeAddresses();
//
//        InputStream is = AwsConsoleApp.class.getResourceAsStream("riak-bootstrap.sh");
//        String userData = readInputStreamAsString(is);
//        userData = new String(Base64.encodeBase64(userData.getBytes()));
//
//        // Create MySQL Instance
//        CreateNewInstance("ami-000000b0", "standard.medium", securityGroups, userData);
        
        cleanupPublicIPs();
    }

    static String join(Collection<String> s, String delimiter)
    {
        StringBuilder builder = new StringBuilder();
        Iterator<String> iter = s.iterator();
        while (iter.hasNext())
        {
            builder.append(iter.next());
            if (!iter.hasNext())
            {
                break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }

    public static void describeImages()
    {
        try
        {
            DescribeImagesResult imagesResult = ec2.describeImages();
            System.out.println("You have access to " + imagesResult.getImages().size() + " Images.");

            for (Image image : imagesResult.getImages())
            {
                System.out.println(image);
            }

        } catch (AmazonServiceException ase)
        {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public static void describeInstances()
    {
        try
        {
            DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
            System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size() + " Availability Zones.");

            for (AvailabilityZone az : availabilityZonesResult.getAvailabilityZones())
            {
                System.out.println(az);
            }

            DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
            List<Reservation> reservations = describeInstancesRequest.getReservations();
            Set<Instance> instances = new HashSet<Instance>();

            for (Reservation reservation : reservations)
            {
                instances.addAll(reservation.getInstances());
            }

            System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");

            for (Instance instance : instances)
            {
                System.out.println(instance);
            }

        } catch (AmazonServiceException ase)
        {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public static void describeVolumes()
    {
        try
        {
            DescribeVolumesResult volumesResult = ec2.describeVolumes();
            System.out.println("You have access to " + volumesResult.getVolumes().size() + " Volumes.");

            for (Volume vol : volumesResult.getVolumes())
            {
                System.out.println(vol);
            }

        } catch (AmazonServiceException ase)
        {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public static void describeKeyPairs()
    {
        DescribeKeyPairsResult result = ec2.describeKeyPairs();
        List<KeyPairInfo> keyPairInfos = result.getKeyPairs();
        for (KeyPairInfo info : keyPairInfos)
        {
            System.out.println(info);
        }
    }

    public static void describeAddresses()
    {
        DescribeAddressesResult result = ec2.describeAddresses();
        List<Address> addresses = result.getAddresses();
        for (Address address : addresses)
        {
            System.out.println(address);
        }
    }

    public static void cleanupPublicIPs()
    {
        DescribeAddressesResult result = ec2.describeAddresses();
        List<Address> addresses = result.getAddresses();

        for (Address address : addresses)
        {

            ReleaseAddressRequest releaseReq = new ReleaseAddressRequest();
            releaseReq.setAllocationId(address.getAllocationId());
            releaseReq.setPublicIp(address.getPublicIp());

            if (address.getInstanceId() == null || address.getInstanceId().equals(""))
            {
                ec2.releaseAddress(releaseReq);
            }
        }
    }

    public static void CreateNewInstance(String imageId, String instanceType, List<String> securityGroups, String userData)
    {
        try
        {
            RunInstancesRequest crir = new RunInstancesRequest();
            crir.setImageId(imageId);

            crir.setInstanceType(instanceType);
            crir.setSecurityGroups(securityGroups);

            crir.setKeyName("hpdefault");

            if (userData != null)
            {
                crir.setUserData(userData);
            }

            RunInstancesResult result = ec2.runInstances(crir);
            System.out.println(result);

            String instanceId = null;
            List<Instance> instances = result.getReservation().getInstances();
            for (Instance instance : instances)
            {
                instanceId = instance.getInstanceId();
            }

            // HACKHACK sleep for 5 seconds so the private ip gets assigned
            System.out.println("Sleeping for 5 to wait for the private ip");
            try
            {
                Thread.sleep(5000);
            } catch (InterruptedException ignore)
            {
                ignore.printStackTrace();
            }

            String publicIp = assignPublicIp(instanceId);

            System.out.println("Public IP: " + publicIp);

            System.out.println("Instance State: " + getInstanceState(instanceId));

        } catch (AmazonServiceException ase)
        {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public static void TerminateInstance(String instanceId)
    {
        try
        {
            TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest();

            List<String> instanceIds = new ArrayList<String>();
            instanceIds.add(instanceId);
            terminateRequest.setInstanceIds(instanceIds);

            TerminateInstancesResult result = ec2.terminateInstances(terminateRequest);

            System.out.println(result);

        } catch (AmazonServiceException ase)
        {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public static void CreateSecurityGroup(String name, String description) throws Exception
    {
        try
        {
            CreateSecurityGroupRequest newSecurityGroup = new CreateSecurityGroupRequest();
            newSecurityGroup.setDescription(description);
            newSecurityGroup.setGroupName(name);

            ec2.createSecurityGroup(newSecurityGroup);

            System.out.println("Security group created : " + name);
        } catch (AmazonServiceException ase)
        {
            System.out.println("Error : Adding new security group");
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public static void DeleteSecurityGroup(String name) throws Exception
    {
        try
        {
            DeleteSecurityGroupRequest deleteSecurityGroupRequest = new DeleteSecurityGroupRequest(name);
            // newSecurityGroup.setDescription(name);
            // newSecurityGroup.setGroupName(description);

            ec2.deleteSecurityGroup(deleteSecurityGroupRequest);

            System.out.println("Security group deleted : " + name);
        } catch (AmazonServiceException ase)
        {
            System.out.println("Error : Adding new security group");
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public static void AddSecurityPorts(List<Integer> ports, String securityGroupName) throws Exception
    {
        for (Integer port : ports)
        {
            try
            {
                AuthorizeSecurityGroupIngressRequest securityPortsRequest = new AuthorizeSecurityGroupIngressRequest();
                securityPortsRequest.setFromPort(port);
                securityPortsRequest.setIpProtocol("tcp");
                securityPortsRequest.setToPort(port);
                securityPortsRequest.setGroupName(securityGroupName);

                ec2.authorizeSecurityGroupIngress(securityPortsRequest);

                System.out.println("Added Access to port " + port.toString());

            } catch (AmazonServiceException ase)
            {
                System.out.println("Error : Adding access to port " + port.toString());
                System.out.println("Caught Exception: " + ase.getMessage());
                System.out.println("Reponse Status Code: " + ase.getStatusCode());
                System.out.println("Error Code: " + ase.getErrorCode());
                System.out.println("Request ID: " + ase.getRequestId());
            }
        }
    }

    public static String assignPublicIp(String instanceId)
    {
        AllocateAddressResult result = ec2.allocateAddress();

        AssociateAddressRequest assRequest = new AssociateAddressRequest();
        assRequest.setAllocationId(result.getAllocationId());
        assRequest.setPublicIp(result.getPublicIp());
        assRequest.setInstanceId(instanceId);

        AssociateAddressResult resp = ec2.associateAddress(assRequest);
        System.out.println(resp);

        return (result.getPublicIp());
    }

    private static String getInstanceState(String instanceId)
    {
        // Fetch status
        DescribeInstancesRequest describeReq = new DescribeInstancesRequest();

        ArrayList<String> ids = new ArrayList<String>();
        ids.add(instanceId);
        describeReq.setInstanceIds(ids);

        String state = null;
        DescribeInstancesResult describeResult = ec2.describeInstances(describeReq);
        List<Reservation> reservations = describeResult.getReservations();
        for (Reservation rez : reservations)
        {
            InstanceState is = rez.getInstances().get(0).getState();
            state = is.getName();
        }

        return (state);
    }

    public static void printAllSecurityGroups() throws Exception
    {
        try
        {
            DescribeSecurityGroupsResult securityResult = ec2.describeSecurityGroups();

            for (SecurityGroup security : securityResult.getSecurityGroups())
            {
                System.out.println(security);
            }

        } catch (AmazonServiceException ase)
        {
            System.out.println("Error : Printing out security group");
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public static void RevokeSecurityPort(int fromPort, int toPort, String securityGroupName) throws Exception
    {
        try
        {
            RevokeSecurityGroupIngressRequest revokeRequest = new RevokeSecurityGroupIngressRequest();
            revokeRequest.setFromPort(fromPort);
            revokeRequest.setIpProtocol("tcp");
            revokeRequest.setToPort(toPort);
            revokeRequest.setGroupName(securityGroupName);

            ec2.revokeSecurityGroupIngress(revokeRequest);

            System.out.println("Security port revoked successfully.  from port (" + fromPort + ") - to port(" + toPort + ")");

        } catch (AmazonServiceException ase)
        {
            System.out.println("Error : revoking security port : from port(" + fromPort + ") - to port(" + toPort + ")");
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public static String readInputStreamAsString(InputStream in) throws IOException
    {
        BufferedInputStream bis = new BufferedInputStream(in);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result = bis.read();
        while (result != -1)
        {
            byte b = (byte) result;
            buf.write(b);
            result = bis.read();
        }
        return buf.toString();
    }
}
