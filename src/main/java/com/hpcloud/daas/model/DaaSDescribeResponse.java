package com.hpcloud.daas.model;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "daas-describe-response")
public class DaaSDescribeResponse
{
    String instanceId;
    String status;
    String ip;
    long port;
    Date launchTime;
    List<Link> links = null;

    public String getInstanceId()
    {
        return instanceId;
    }

    public void setInstanceId(String instanceId)
    {
        this.instanceId = instanceId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getIp()
    {
        return ip;
    }

    public void setIp(String ip)
    {
        this.ip = ip;
    }

    public long getPort()
    {
        return port;
    }

    public void setPort(long port)
    {
        this.port = port;
    }

    public List<Link> getLinks()
    {
        return links;
    }

    public void setLinks(List<Link> links)
    {
        this.links = links;
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
