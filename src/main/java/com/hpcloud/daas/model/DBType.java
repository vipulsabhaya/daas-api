package com.hpcloud.daas.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "dbtype")
public class DBType
{
    String name;
    String version;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }
}
