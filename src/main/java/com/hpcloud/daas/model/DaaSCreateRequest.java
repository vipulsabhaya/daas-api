package com.hpcloud.daas.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "daas-create-request")
public class DaaSCreateRequest
{
    String name;
    String flavor;
    DBType dbType;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getFlavor()
    {
        return flavor;
    }

    public void setFlavor(String flavor)
    {
        this.flavor = flavor;
    }

    public DBType getDbType()
    {
        return dbType;
    }

    public void setDbType(DBType dbType)
    {
        this.dbType = dbType;
    }
}
