package com.example.struts;

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

public class ServerLauncher {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        String portProp = System.getProperty("server.port");
        if (portProp == null || portProp.trim().isEmpty()) {
            portProp = System.getenv("PORT");
        }
        if (portProp != null && !portProp.trim().isEmpty()) {
            port = Integer.parseInt(portProp.trim());
        }

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector(); // Initialize default HTTP connector

        File baseDir = new File("target/tomcat");
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        tomcat.setBaseDir(baseDir.getAbsolutePath());

        File webappDir = new File("src/main/webapp");
        String contextPath = "/struts1-app";

        Context context = tomcat.addWebapp(contextPath, webappDir.getAbsolutePath());
        context.setParentClassLoader(ServerLauncher.class.getClassLoader());

        // Setup resources so WEB-INF/classes points to target/classes
        File additionWebInfClasses = new File("target/classes");
        if (additionWebInfClasses.exists()) {
            WebResourceRoot resources = new StandardRoot(context);
            resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                    additionWebInfClasses.getAbsolutePath(), "/"));
            context.setResources(resources);
        }

        new com.example.struts.db.CustomerDao().initSchemaAndSeed();
        new com.example.struts.db.DocumentDao().initSchemaAndSeed();
        org.h2.tools.Server webConsole =
                org.h2.tools.Server.createWebServer("-webPort", "8082").start();
        Runtime.getRuntime().addShutdownHook(new Thread(webConsole::stop));

        System.out.println("=================================================");
        System.out.println("🚀 Starting Struts 1 on Java " + System.getProperty("java.version"));
        System.out.println("🌐 URL: http://localhost:" + port + contextPath + "/welcome.do");
        System.out.println("🗄️  H2 Console: http://localhost:8082");
        System.out.println("=================================================");

        tomcat.start();
        tomcat.getServer().await();
    }
}
