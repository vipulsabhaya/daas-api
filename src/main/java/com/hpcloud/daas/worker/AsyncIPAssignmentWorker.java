package com.hpcloud.daas.worker;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.hpcloud.daas.ec2.EC2Client;
import com.hpcloud.daas.ec2.InstanceResult;

public class AsyncIPAssignmentWorker implements Runnable
{
    String instanceId;
    AWSCredentials credentials;
    
    public AsyncIPAssignmentWorker( AWSCredentials creds, String id )
    {
        instanceId = id;
        credentials = creds;
    }
    
    public void run ()
    {
        Logger logger = Logger.getLogger( this.getClass() );
        
        logger.info( "BEGIN IP Assignment" );
        EC2Client client = new EC2Client( credentials );
        
        InstanceResult ir = client.assignPublicIp( instanceId );
        
        logger.info( "END IP Assignment: " + ir.getPublicIp() );
        
        try 
        {
            WebClient webClient = WebClient.create("http://localhost:8080" );
            webClient.path("oaas/v1.0/server/" + instanceId).post("{ \"ip\" : \""+ ir.getPublicIp() +"\" " );
        }
        catch(Throwable t)
        {
            logger.fatal("Failed invoking callback to oaas", t);
        }
    }

}
