package com.hpcloud.daas.ec2;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AssociateAddressResult;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.hpcloud.daas.listener.DBaaS;

public class EC2Client
{
    private static final Log logger = LogFactory.getLog(EC2Client.class);

    // Miscellaneous constants
    public static final String US_WEST2_AZ2_EC2_ENDPOINT = "https://az-2.region-a.geo-1.ec2-compute.hpcloudsvc.com/services/Cloud/";
    public static final String DEFAULT_KEYPAIR = "hpdefault";

    final protected AmazonEC2 ec2;

    public EC2Client(String pubkey, String seckey)
    {
        this(new BasicAWSCredentials(pubkey, seckey));
    }

    public EC2Client(AWSCredentials credentials)
    {
        ec2 = new AmazonEC2Client(credentials);
        ec2.setEndpoint(US_WEST2_AZ2_EC2_ENDPOINT);
    }

    public InstanceResult createNewInstance(String kernelId, String imageId, String instanceType, List<String> securityGroups, String userData) throws Exception
    {
        try
        {
            RunInstancesRequest crir = new RunInstancesRequest();
            crir.setImageId(imageId);
            crir.setKernelId(kernelId);
            crir.setInstanceType(instanceType);
            crir.setSecurityGroups(securityGroups);
            crir.setKeyName(DEFAULT_KEYPAIR);
            crir.setUserData(userData);

            RunInstancesResult result = ec2.runInstances(crir);
            logger.info(result);
            
            List<Instance> instances = result.getReservation().getInstances();
            String instanceId = instances.get(instances.size() - 1).getInstanceId();
            String state = getInstanceState(instanceId);

            InstanceResult instanceResult = null;
            instanceResult = new InstanceResult();
            instanceResult.setInstanceId(instanceId);
            instanceResult.setStatus(state);
            return instanceResult;

        } catch (AmazonServiceException ase)
        {
            logger.error("Caught Exception: " + ase.getMessage());
            logger.error("Reponse Status Code: " + ase.getStatusCode());
            logger.error("Error Code: " + ase.getErrorCode());
            logger.error("Request ID: " + ase.getRequestId());
            throw new Exception("Error creating new instance: " + ase.getMessage(), ase);
        } catch (Exception e)
        {
            e.printStackTrace();
            throw new Exception("Error creating new instance: " + e.getMessage(), e);
        }
    }

    public InstanceResult assignPublicIp(String instanceId)
    {
        AllocateAddressResult result = ec2.allocateAddress();

        AssociateAddressRequest assRequest = new AssociateAddressRequest();
        assRequest.setAllocationId(result.getAllocationId());
        assRequest.setPublicIp(result.getPublicIp());
        assRequest.setInstanceId(instanceId);

        AssociateAddressResult resp = ec2.associateAddress(assRequest);

        InstanceResult instanceResult = new InstanceResult();
        instanceResult.setInstanceId(instanceId);
        instanceResult.setStatus( getInstanceState( instanceId ) );
        instanceResult.setPublicIp( result.getPublicIp() );

        return instanceResult;
    }

    public void createSecurityGroup(String name, List<Integer> ports, String description) throws Exception
    {
        CreateSecurityGroupRequest newSecurityGroup = new CreateSecurityGroupRequest();
        newSecurityGroup.setDescription(description);
        newSecurityGroup.setGroupName(name);

        ec2.createSecurityGroup(newSecurityGroup);

        for (Integer port : ports)
        {
            AuthorizeSecurityGroupIngressRequest securityPortsRequest = new AuthorizeSecurityGroupIngressRequest();

            securityPortsRequest.setFromPort(port);
            securityPortsRequest.setIpProtocol("tcp");
            securityPortsRequest.setToPort(port);
            securityPortsRequest.setGroupName(name);
            ec2.authorizeSecurityGroupIngress(securityPortsRequest);
        }
    }

    private String getInstanceState(String instanceId)
    {
        // Fetch status
        DescribeInstancesRequest describeReq = new DescribeInstancesRequest();

        List<String> ids = Arrays.asList(instanceId);
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

    public InstanceResult getInstanceDetails(String instanceId)
    {
        // Fetch status
        DescribeInstancesRequest describeReq = new DescribeInstancesRequest();

        List<String> ids = Arrays.asList(instanceId);
        describeReq.setInstanceIds(ids);

        InstanceResult instanceResult = new InstanceResult();

        DescribeInstancesResult describeResult = ec2.describeInstances(describeReq);
        List<Reservation> reservations = describeResult.getReservations();
        for (Reservation rez : reservations)
        {
            instanceResult.setLaunchTime(rez.getInstances().get(0).getLaunchTime());
            instanceResult.setPublicIp(rez.getInstances().get(0).getPublicIpAddress());
            instanceResult.setInstanceId(instanceId);
            instanceResult.setStatus(rez.getInstances().get(0).getState().getName());
        }

        return instanceResult;
    }

    public InstanceResult TerminateInstance(String instanceId)
    {
        InstanceResult instanceResult = new InstanceResult();
        
        TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest();

        List<String> instanceIds = Arrays.asList(instanceId);
        terminateRequest.setInstanceIds(instanceIds);

        TerminateInstancesResult result = ec2.terminateInstances(terminateRequest);

        List<InstanceStateChange> terminating = result.getTerminatingInstances();
        if(terminating.size() > 0)
        {
            instanceResult.setStatus(terminating.get(0).getCurrentState().getName());
        }
        else
        {
            instanceResult.setStatus("terminated");
        }
            
        instanceResult.setInstanceId(instanceId);
        
        return instanceResult;
    }
    
    public static void main(String[] args) throws Exception
    {
        EC2Client client = new EC2Client("ec2_access_key", "ec2_secret_key");
     
        String image = "ami-000000b0";
        String kernel = "aki-000000af";
        String flavor = "standard.medium";
        List<String> secGroups = Arrays.asList("default"); 
        InputStream is = AwsConsoleApp.class.getResourceAsStream("riak-bootstrap.sh");
        
        String userData = AwsConsoleApp.readInputStreamAsString(is);
        userData = new String(Base64.encodeBase64(userData.getBytes()));
        InstanceResult instanceResult = client.createNewInstance(kernel, image, flavor, secGroups, userData);   
        System.out.println("All done");
    }
}
