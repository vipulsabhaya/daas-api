package com.hpcloud.daas.ec2;

import java.util.Date;

public class InstanceResult
{
    String instanceId;
    String publicIp;
    String status;
    Date launchTime;

    public String getInstanceId()
    {
        return instanceId;
    }

    public void setInstanceId(String instanceId)
    {
        this.instanceId = instanceId;
    }

    public String getPublicIp()
    {
        return publicIp;
    }

    public void setPublicIp(String publicIp)
    {
        this.publicIp = publicIp;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getLaunchTime()
    {
        return launchTime;
    }

    public void setLaunchTime(Date launchTime)
    {
        this.launchTime = launchTime;
    }
}
