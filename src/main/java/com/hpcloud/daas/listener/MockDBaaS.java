package com.hpcloud.daas.listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.hpcloud.daas.ec2.InstanceResult;
import com.hpcloud.daas.ec2.MockEC2Client;
import com.hpcloud.daas.model.DaaSCreateRequest;
import com.hpcloud.daas.model.DaaSCreateResponse;
import com.hpcloud.daas.model.DaaSDescribeResponse;
import com.hpcloud.daas.model.DaaSDestroyResponse;
import com.hpcloud.daas.model.Link;

@Path("/test")
public class MockDBaaS
{
    private static final String DEFAULT_FLAVOR = "standard.large";
    private static final String DEFAULT_IMAGE = "ami-000000a4";

    private final AWSCredentials credentials;
    private final MockEC2Client client;

    public MockDBaaS()
    {
        try
        {
            credentials = new PropertiesCredentials(MockDBaaS.class.getClassLoader().getResourceAsStream("AwsCredentials.properties"));
            client = new MockEC2Client(credentials);
        } catch (IOException e)
        {
            throw new RuntimeException("Error loading AWS Credentials");
        }
    }

    @POST
    @Path("/instance")
    @Consumes(
    { "application/xml", "application/json" })
    public DaaSCreateResponse createDatabaseInstance(DaaSCreateRequest request)
    {
        DaaSCreateResponse response = new DaaSCreateResponse();

        String flavor = request.getFlavor();
        if (flavor == null || flavor.equals(""))
        {
            flavor = DEFAULT_FLAVOR;
        }
        String image = DEFAULT_IMAGE;

        List<String> secGroups = new ArrayList<String>();
        secGroups.add("default");
        secGroups.add("MySQL-secgroup");

        InstanceResult result = client.createNewInstance(image, flavor, secGroups, null);

        // Mock Result
        response.setInstanceId(result.getInstanceId());
        response.setName(request.getName() + "_" + result.getInstanceId());
        response.setIp(result.getPublicIp());
        response.setUsername("root");
        response.setPassword("hpcs");
        response.setPort(3306);
        response.setStatus(result.getStatus());

        List<Link> linkList = new ArrayList<Link>();
        Link link = new Link();
        link.setHref("https://localhost:8080/daas/test/instance/" + result.getInstanceId());
        link.setRel("self");
        linkList.add(link);
        response.setLinks(linkList);

        return response;
    }

    @DELETE
    @Path("/instance/{instanceId}")
    @Consumes(
    { "application/xml", "application/json" })
    public DaaSDestroyResponse destroyDatabaseInstance(@PathParam("instanceId") String instanceId)
    {
        DaaSDestroyResponse response;

        return (null);
    }

    @GET
    @Path("/instance/{instanceId}")
    @Consumes(
    { "application/xml", "application/json" })
    public DaaSDescribeResponse describeDatabaseInstance(@PathParam("instanceId") String instanceId)
    {
        DaaSDescribeResponse response = new DaaSDescribeResponse();
        try
        {
            InstanceResult result = client.getInstanceDetails(instanceId);
            response.setInstanceId(result.getInstanceId());
            response.setIp(result.getPublicIp());
            response.setPort(3306);
            response.setStatus(result.getStatus());
            response.setLaunchTime(result.getLaunchTime());

            List<Link> linkList = new ArrayList<Link>();
            Link link = new Link();
            link.setHref("https://localhost:8080/daas/test/instance/" + result.getInstanceId());
            link.setRel("self");
            linkList.add(link);
            response.setLinks(linkList);
        } catch (Exception e)
        {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    }
}
