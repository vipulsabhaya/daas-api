package com.hpcloud.daas.listener;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.log4j.Logger;

@WebListener
public class DaaSServletContextListener implements ServletContextListener
{
    public DaaSServletContextListener()
    {

    }

    // -------------------------------------------------------
    // ServletContextListener implementation
    // -------------------------------------------------------
    public void contextInitialized(ServletContextEvent sce)
    {
        Logger logger = Logger.getLogger(getClass());
        logger.info("DaaSServletContextListener contextInitialized()");

        // 2. Creation of a global async Executor
        Executor executor = new ThreadPoolExecutor(10, 10, 50000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100));
        sce.getServletContext().setAttribute("executor", executor);
    }

    public void contextDestroyed(ServletContextEvent sce)
    {
        Logger logger = Logger.getLogger(getClass());

        logger.info("DaaSServletContextListener contextDestroyed()");

        ThreadPoolExecutor executor = (ThreadPoolExecutor) sce.getServletContext().getAttribute("executor");
        executor.shutdown();
    }
}
