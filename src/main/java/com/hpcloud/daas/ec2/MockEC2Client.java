package com.hpcloud.daas.ec2;

import java.util.Date;
import java.util.List;
import java.util.Random;

import com.amazonaws.auth.AWSCredentials;

public class MockEC2Client
{
    public MockEC2Client(AWSCredentials credentials)
    {

    }

    public InstanceResult createNewInstance(String imageId, String instanceType, List<String> securityGroups, String userData)
    {
        InstanceResult ret = new InstanceResult();

        Random rand = new Random();
        int id = rand.nextInt(100);
        String instanceId = "i-0000aa" + id;

        ret.setInstanceId(instanceId);
        ret.setStatus("networking");

        return (ret);
    }

    public InstanceResult getInstanceDetails(String instanceId)
    {
        InstanceResult result = new InstanceResult();

        result.setLaunchTime(new Date());
        result.setPublicIp("11.11.11.11");
        result.setInstanceId(instanceId);
        result.setStatus("running");

        return result;
    }
}
