Fenix Webapp IST
===

**Table of Contents**

- [About](#about)
- [Setup Environment](#setup-environment)
- [Compiling](#compiling)
- [Create Jar for Fenix Scripts](#create-jar-for-fenix-scripts)
- [Create Manual SQL Updates](#create-manual-sql-updates)


#About

Fenix Webapp is the aggregator project for IST's Fenix Installation.


#Setup Environment

1. Create a configuration file using `mvn bennu:generate-configuration`. This generates `src/main/resources/configuration.properties`, which should be customized to best suit your needs.
2. Copy `src/main/resources/fenix-framework.properties.sample` to `src/main/resources/fenix-framework.properties`.
3. Modify both files accordingly
4. `export JAVA_OPTS="-server -Xms256m -Xmx1024m -XX:PermSize=384m"`
5. `export MAVEN_OPTS="$JAVA_OPTS"`


#Compiling

There are three ways to compile the Fenix Webapp.

## Embedded 

This is the approach best suited for development. Instead of packaging the Webapp, it is possible to run Tomcat directly in Maven.

To run the webapp, using `tomcat7-maven-plugin`, add the following to your `MAVEN_OPTS` environment property:

    -Dorg.apache.jasper.compiler.Parser.STRICT_QUOTE_ESCAPING=false

And then, run the webapp using:

    mvn clean tomcat7:run

or
    
    mvn tomcat7:run
    
To run the embedded Tomcat with the Debugger, add the followwing to your `MAVEN_OPTS` environment property:

    -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n
    
You can change the `suspend=n` to `suspend=y` if debugging the startup process is required.

## Exploded War

This approach is well suited for development, as it does not create a War file.

    mvn clean prepare-package war:exploded
    
This creates the exploded Webapp in `target/fenix-webapp-1.0.0-SNAPSHOT`.

To run the Webapp in a standalone Tomcat installation, simply create a Symlink using:

    ln -s $(pwd)/target/fenix-webapp-1.0.0-SNAPSHOT <tomcat>/webapps/fenix
    
## War File

This approach is the one used for production environments. Simply run:

    mvn clean package
    
The generated War file is located in `target/fenix-webapp-1.0.0-SNAPSHOT.war`


# Create Jar for Fenix Scripts

To create the Jar required for Fenix Scripts (Which contains the necessary Base classes and configuration files), simply run:

    mvn clean test -Pjar
    
This creates `deploy/fenix-webapp-1.0.0-SNAPSHOT.jar`


# Create Manual SQL Updates

Schema updates are now automatically handled. However, if you find yourself in need of meddling with the schema update process, and need to know what the Framework is doing, you can run:

    mvn clean test -PSQLUpdateGenerator
    
Which will create the file `etc/database_operations/updates.sql`
