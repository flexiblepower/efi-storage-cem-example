# EFI Storage CEM example

This project is an example implementation for a *Customer Energy Manager* (CEM) for the *Energy Flexibility Interface* (EFI) Storage category. This example implementation uses WebSockets to communicate with the *Resource Manager* (RM) of a Storage device.

### Introduction

The way we use energy is evolving. The introduction of intermittent renewable energy sources such as wind energy and solar energy, and new electricity intensive devices such as electric vehicles and electric heating for buildings, makes it difficult to keep the power grid in balance. Using the energy flexibility of devices and clever software, we can shift the energy consumption and production to other times to balance the grid. Unfortunately there are many types, brands and models of devices, each with their own specific protocols, which makes it hard for software to interact with all these devices to utilze the energy flexibility. There are simply too many protocols out there to support for one piece of software.

The Energy Flexibility Interface (EFI) fixes this problem by introducing a protocol which purely focuses on energy flexibility, instead of a protocol which implements all the features of a device. A device is represented by a piece of software called the Resource Manager (RM), which translates the device specific interface into EFI. Using EFI, the Resource Manager can communicate with a Customer Energy Manager (CEM). The CEM makes decisions on how to utilize the energy flexibility of the device. With EFI, the device is always in charge, making sure the device is not damaged and user comfort is guaranteed. Typically, each building has a CEM. A CEM can communicate with many Resource Managers, but a Resource Manager always communicates with one CEM.

EFI consist out of four sub protocols called categories: Inflexible, Shiftable, Adjustable and Storage. Each category is used to model a different kind of energy flexibility. This application only implements the Storage category. It does not support all this EFI Storage features.

EFI is an XML based, message based protocol. It uses bi-directional asynchronous messaging. This means that both the CEM and the RM can send a message whenever they want. The EFI XML messages are formalized using XSD.

Technical resources of EFI can be found on [Github](https://github.com/flexiblepower/efi).

### Purpose of this application

This application can be used for demonstration purposes, as a starting point for creating a CEM implementation or as inspiration for how to implement a CEM in another programming language. Please bare in mind that this example is not complete; not all EFI Storage features are implemented.

### Used technologies
Apache Maven to download dependencies, generate code and to build the application. JAXB is used for parsing and generating XML. Jetty is used as the websocket client. The graphical user interface is implented using Java Swing.

## Required software

In order to build this application you need:

* [Java 1.8 SDK or higher](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) (a.k.a. The Java 8 SE Development Kit)
* [Apache Maven](https://maven.apache.org/download.cgi)
* An internet connection

In order to run this appplication you need:

* [Java 1.8 JRE or higher](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* The URL of an EFI Resource Manager which you can talk to using a WebSocket

## Building the application

After checking out the software using git, or downloading it, go into the project directory using your command line:

```
cd efi-storage-cem-example/
```

The first thin to do is to generate use JAXB to generate Java classes for the XML elements. JAXB uses the EFI [XSD](https://en.wikipedia.org/wiki/XML_Schema_(W3C)) file to generate Java classes. This way it becomes easier to parse and create XML messages is Java. Without these generated classes, the code would not compile. The XSD is already part of the project. Since Maven has to download some components, you need to have an Internet connection. Generate the classes using the following Maven command:

```
mvn generate-sources
```

The generated classes can be found in the `/target/generated-sources/cxf-xjc` directory.

The next step is compile the application. The application can be compiled into a *fat jar*. A jar is a zip archive which contains a Java application. However, typically a jar does not include any used third-party libraries. A fat jar does include all the user libraries, so it is the only file you need to run the application. You can build the file using the following Maven command:

```
mvn package
```

The jar of the application can be found at `/target/storage-cem-example-0.0.1-SNAPSHOT-jar-with-dependencies.jar`.

## Running the application

In order to run the application, you need to have the URL of an EFI Resource Manager which you can access through a WebSocket. If the URL is for example `ws://device.com/storage`, you can start the application using the following command:

```
java -jar java -jar target/storage-cem-example-0.0.1-SNAPSHOT-jar-with-dependencies.jar ws://device.com/storage
```
