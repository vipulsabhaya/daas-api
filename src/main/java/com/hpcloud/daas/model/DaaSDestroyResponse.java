package com.hpcloud.daas.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="daas-terminate-response")
public class DaaSDestroyResponse
{
    private String status;

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
