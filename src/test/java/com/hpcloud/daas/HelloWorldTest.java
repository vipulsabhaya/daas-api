package com.hpcloud.daas;

import org.hamcrest.Matchers;
import org.junit.Test;

import com.amazonaws.services.ec2.model.RunInstancesRequest;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class HelloWorldTest
{
    @Test
    public void helloWorldTest()
    {
        String helloWorld = "hello world";
        assertThat(helloWorld, equalTo("hello world"));
    }
    
    @Test
    public void runInstanceRequestUserDataDefaultsToNull()
    {
        RunInstancesRequest request = new RunInstancesRequest();
        assertThat(request.getUserData(), nullValue());
    }

}
