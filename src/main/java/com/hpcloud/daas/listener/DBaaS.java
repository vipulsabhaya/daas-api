package com.hpcloud.daas.listener;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.hpcloud.daas.ec2.EC2Client;
import com.hpcloud.daas.ec2.InstanceResult;
import com.hpcloud.daas.model.DaaSCreateRequest;
import com.hpcloud.daas.model.DaaSCreateResponse;
import com.hpcloud.daas.model.DaaSDescribeResponse;
import com.hpcloud.daas.model.DaaSDestroyResponse;
import com.hpcloud.daas.model.Link;
import com.hpcloud.daas.worker.AsyncIPAssignmentWorker;

public class DBaaS
{
    private static final Logger logger = Logger.getLogger(DBaaS.class);
    
    // Miscellaneous constants
    public static final String DBAAS_ROOT_URI = "http://localhost:8080/daas";
    public static final String OAAS_ROOT_URI = "http://15.185.163.32/oaas/v1.0";
    
    // MySQL related constants
    public static final int DEFAULT_MYSQL_PORT = 3306;
    public static final String DEFAULT_MYSQL_PASSWORD = "hpcs";
    public static final String DEFAULT_MYSQL_USER = "root";
    public static final String DEFAULT_SECURITY_GROUP = "default";
    public static final String MYSQL_SECURITY_GROUP = "MySQL-secgroup";
    
    // Riak related constants
    // TODO: Make a separate riak security group?
    public static final int DEFAULT_RIAK_PORT = 8098;
    public static final String RIAK_SECURITY_GROUP = "default";
    
    private final AWSCredentials credentials;
    private final EC2Client client;
    ServletContext context;
    
    private final Map<String, String> instanceIdToDbType = new ConcurrentHashMap<String, String>();
    
    public DBaaS()
    {
        try
        {
            credentials = new PropertiesCredentials(DBaaS.class.getClassLoader().getResourceAsStream("AwsCredentials.properties"));
            client = new EC2Client(credentials);
        } catch (IOException e)
        {
            throw new RuntimeException("Error loading AWS Credentials");
        }
    }
    
    public DBaaS(@Context ServletContext ctx )
    {
        context = ctx;
        try
        {
            credentials = new PropertiesCredentials(DBaaS.class.getClassLoader().getResourceAsStream("AwsCredentials.properties"));
            client = new EC2Client(credentials);
        } catch (IOException e)
        {
            throw new RuntimeException("Error loading AWS Credentials");
        }
    }

    @POST
    @Path("/instance")
    @Consumes(
    { "application/xml", "application/json" })
    // TODO: stop ioexception from propogating upa 
    public Response createDatabaseInstance(DaaSCreateRequest request) throws IOException
    {
        DaaSCreateResponse response = new DaaSCreateResponse();
        
        String databaseType = "mysql";
        // Null safety is hard, let's go shopping!
        if (request != null && request.getDbType() != null && request.getDbType().getName() != null)
        {
            // Safeguard: only allow riak through
            if (request.getDbType().getName().equals("riak"))
            {
                databaseType = request.getDbType().getName();
            }
        }
        
        String flavor = getInstanceFlavor(databaseType);
        String kernel = getInstanceKernel(databaseType);
        String image = getInstanceImage(databaseType);
        List<String> secGroups = getSecurityGroups(databaseType);
        String userData = getBootScriptUserData(databaseType);
        
        logger.info("databaseType: " + databaseType);
        logger.info("flavor: " + flavor);
        logger.info("kernel: " + kernel);
        logger.info("image: " + image);
        logger.info("groups: " + secGroups);
        logger.info("userData: " + userData);

        URI location = null;
        try
        {
            InstanceResult instanceResult = client.createNewInstance(kernel, image, flavor, secGroups, userData);
            instanceIdToDbType.put(instanceResult.getInstanceId(), databaseType);
            response.setInstanceId(instanceResult.getInstanceId());
            response.setName(request.getName() + "_" + instanceResult.getInstanceId());
            response.setIp(instanceResult.getPublicIp());
            if (databaseType != null && databaseType.equals("riak"))
            {
                response.setPort(DEFAULT_RIAK_PORT);
            }
            else
            {
                response.setUsername(DEFAULT_MYSQL_USER);
                response.setPassword(DEFAULT_MYSQL_PASSWORD);
                response.setPort(DEFAULT_MYSQL_PORT);
            }
            response.setStatus(instanceResult.getStatus());

            List<Link> linkList = createInstanceLinkList(instanceResult);
            response.setLinks(linkList);

            location = new URI(linkList.get(0).getHref());
            
            assignPublicIP( instanceResult.getInstanceId() );
        } catch (Exception e)
        {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.created(location).entity(response).build();
    }

    @DELETE
    @Path("/instance/{instanceId}")
    @Consumes(
    { "application/xml", "application/json" })
    public DaaSDestroyResponse destroyDatabaseInstance(@PathParam("instanceId") String instanceId)
    {
        DaaSDestroyResponse response = new DaaSDestroyResponse();
        try
        {
            InstanceResult instanceResult = client.TerminateInstance( instanceId );
            instanceIdToDbType.remove(instanceId);
            response.setStatus(instanceResult.getStatus());
        }
        catch (Exception e)
        {
            logger.fatal("Failed to terminate instance", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @GET
    @Path("/instance/{instanceId}")
    @Consumes(
    { "application/xml", "application/json" })
    public DaaSDescribeResponse describeDatabaseInstance(@PathParam("instanceId") String instanceId)
    {
        logger.info("instanceId: " + instanceId);
        DaaSDescribeResponse response = new DaaSDescribeResponse();
        String databaseType = instanceIdToDbType.get(instanceId);
        try
        {
            InstanceResult result = client.getInstanceDetails(instanceId);
            response.setInstanceId(result.getInstanceId());
            response.setIp(result.getPublicIp());
            response.setStatus(result.getStatus());
            response.setLaunchTime(result.getLaunchTime());
            if (databaseType != null && databaseType.equals("riak"))
            {
                response.setPort(DEFAULT_RIAK_PORT);
            }
            else
            {
                response.setPort(DEFAULT_MYSQL_PORT);
            }

            List<Link> linkList = createInstanceLinkList(result);
            response.setLinks(linkList);
        } catch (Exception e)
        {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    protected void assignPublicIP( String instanceId )
    {
        Executor executor = (Executor) context.getAttribute( "executor" );
        AsyncIPAssignmentWorker worker = new AsyncIPAssignmentWorker( credentials, instanceId );
        executor.execute( worker );
    }
    
    protected List<Link> createInstanceLinkList(InstanceResult instanceResult)
    {
        return Arrays.asList(new Link("self", DBAAS_ROOT_URI + "/instance/" + instanceResult.getInstanceId()));
    }

    protected String readInputStreamAsString(InputStream in) throws IOException
    {
        BufferedInputStream bis = new BufferedInputStream(in);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result = bis.read();
        while (result != -1)
        {
            byte b = (byte) result;
            buf.write(b);
            result = bis.read();
        }
        return buf.toString();
    }
    
    // ------------------------------------------------
    // Stuff that is parameterized on the database type
    
    protected String getInstanceFlavor(String databaseType)
    {
        Map<String, String> map = new HashMap<String, String>();
        map.put("mysql", "standard.large");
        map.put("riak", "standard.medium");
        return map.get(databaseType);
    }
    
    protected String getInstanceImage(String databaseType)
    {
        Map<String, String> map = new HashMap<String, String>();
        map.put("mysql", "ami-000000a4");
        map.put("riak", "ami-000000b0");
        return map.get(databaseType);
    }
    
    protected String getInstanceKernel(String databaseType)
    {
        Map<String, String> map = new HashMap<String, String>();
        map.put("mysql", "aki-000000a3");
        map.put("riak", "aki-000000af");
        return map.get(databaseType);
    }
    
    protected List<String> getSecurityGroups(String databaseType)
    {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        map.put("mysql", Arrays.asList(DEFAULT_SECURITY_GROUP, MYSQL_SECURITY_GROUP));
        map.put("riak", Arrays.asList(DEFAULT_SECURITY_GROUP, RIAK_SECURITY_GROUP));
        return map.get(databaseType);
    }
    
    protected String getBootScriptUserData (String databaseType) throws IOException
    {
        String userData = null;

        InputStream is = DBaaS.class.getClassLoader().getResourceAsStream( "com/hpcloud/daas/ec2/"+databaseType+"-bootstrap.sh" );
        if( is == null )
        {
            throw new NullPointerException("bootstrap script not found");
            
        }
        userData = readInputStreamAsString( is );
        
        // Give the bootstrap script a url to post for success (what's a template?)
        userData = Pattern.compile("OAAS_URL=").matcher(userData).replaceAll("OAAS_URL="+OAAS_ROOT_URI);
        
        userData = new String( Base64.encodeBase64( userData.getBytes() ) );
        
        return (userData);
    }
}
