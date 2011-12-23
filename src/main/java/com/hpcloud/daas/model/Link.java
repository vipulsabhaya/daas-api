package com.hpcloud.daas.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "link")
public class Link
{
    String href;
    String rel;
    
    public Link()
    {
        
    }
    
    public Link (String rel, String href)
    {
        this.rel = rel;
        this.href = href;
    }

    public String getHref()
    {
        return href;
    }

    public void setHref(String href)
    {
        this.href = href;
    }

    public String getRel()
    {
        return rel;
    }

    public void setRel(String rel)
    {
        this.rel = rel;
    }
}
