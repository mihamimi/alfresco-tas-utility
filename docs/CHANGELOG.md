# Change Log
All notable changes to this project will be documented in this file.

## [[v1.0.0] - 2016-11-18](commits/v1.0.0)
### Added

- ability to use models of Alfresco objects in your automation test
- ability to create workflows
- helpers for generating random data
- ability to create data based on XML file (site structure). See XMLTestDataProvider.java
- ability to create Users in repository
- ability to create data users

```java
@Autowired
DataUser userData;
userData.createRandomTestUser();

//or explicit specified by you:
UserModel myUser = new UserModel("tas-user", "password");
userData.createUser(myUser);
```

- ability to create Data in repository based only on alfresco IP address and admin provided

```java
@Autowired
DataContent data;
data.createFolder(FolderModel folderModel)

//where the folderModel is generated
folderModel = FolderModel.getRandomFolderModel() 

//or explicit specified by you:
folderModel = new FolderModel("myFolder")
```

- ability to create Sites in repository based only on alfresco IP address and admin provided

```java
@Autowired
DataSite data;
data.createSite(SiteModel siteModel)

//where the siteModel is generated
siteModel = SiteModel.getRandomSiteModel();

//or explicit specified by you:
siteModel = new SiteModel("myNewSite")
```

- ability to use Groups

```java
@Autowired
DataGrou dataGroup;
dataGroup.usingUser(testUser).addUserToGroup(GroupModel.getEmailContributorsGroup());
```
- ability to test extension points
If you are implementing a [custom extension](http://docs.alfresco.com/5.1/concepts/dev-platf…) point you can also test it, outside the repository.
   * just define your GET url (let's say /myExtentionTest) with one freemaker  template that will generate this xml file:
  
```xml
<?xml version="1.0" encoding="UTF-8"?>
<ExtensionPointTestSuite>
	<name>${className}</name>
	<description>${classNameDesc}</description>
	<TestCases>
	<#if testCases??>
   			<#list testCases as response>
   				<testcase name="${response.methodName?js_string}">
					<description>${response.methodNameDesc?js_string}</description>
					<duration>1</duration>
					<actualValue>${response.value}</actualValue>
					<expectedValue>${response.expectedValue}</expectedValue>
					<stackTrace>${response.stackTrace?js_string}</stackTrace>
				</testcase>
   			   <#if (response_has_next)></#if>
   		    </#list>
	</#if>
  </TestCases>
</ExtensionPointTestSuite>  
```

(take a look at [alfresco-tas-contentmodel-extension](https://gitlab.alfresco.com/tas/alfresco-tas-contentmodel-extension) implementation)

- ability of asserting if particular extension point is installed  

```java
  dataContent.assertExtensionAmpExists("<artifact-id-of-extention-point>")
```

- ability to check the server health of Alfresco installed based only on IP and admin user provided.

  ```java
  @Autowired ServerHealth serverHealth;
  ...
  serverHealth.isAlfrescoRunning() will return true if:
     * we can reach the IP provided
     * we can access the Admin System Summary Page (basic authenticating with admin provided) 
  ```

- ability to report test results in a HTML format
   * just add as listener to your TestNG test: HtmlReportListener.class

   ```
   @Listener(value=org.alfresco.utility.report.HtmlReportListener.class)
   ``` 
   
   * or add as listener to your TestNG suite xml file (approach that we embrace)
   
   ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
    <suite name="MySuite">
    	<listeners>
    		<listener class-name="org.alfresco.utility.report.HtmlReportListener"></listener>
    	</listeners>
    	(....)
    </suite> <!-- Suite -->
   ``` 

- ability to report test results into test tracker (TestRail)
   * just add annotation: @TestRail to your test (add appropiate section that already exist in TestRail)
   * just add as listener to your TestNG test: TestRailExecutorListener.clas
   
   ```
   @Listener(value=org.alfresco.utility.testrail.TestRailExecutorListener.class)
   ``` 
   
   * or add as listener to your TestNG suite xml file (approach that we embrace)

   ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
    <suite name="MySuite">
    	<listeners>
    		<listener class-name="org.alfresco.utility.testrail.TestRailExecutorListener.class"></listener>
    	</listeners>
    	(....)
    </suite> <!-- Suite -->
   ``` 
- ability to document steps in your @Test using STEP helper (info are passed on test tracker too)   
- ability to annotate issues in your tests

   ```java
   @Bug(id="ACE-1")
   @Test
   public void myTest()
   {
    (...)
   }

   ```

  (this issue will be visible in HTML report (if HtmlReportListener is used) or in TestRail (issue added as Defect) if TestRailExecutorListener is used)

- ability to call JMX on Alfresco server (if alfresco is running in docker or behind a firewall)

  We take advantage of https://jolokia.org agent to interact with JMX calls on server side. 
  Docker provisioning will automatically installed Jolokia agent on each Stack, so we can use this instead of direct JMX calls.

  This feature is configurable from default.properties file (just enable of disable it)

  ```
   # in containers we cannot access directly JMX, so we will use http://jolokia.org agent
   # disabling this we will use direct JMX calls to server
   jmx.useJolokiaAgent=true
  ```
  In the code you will use something like:

  ```java
    @Autowired
    JmxBuilder jmx;
    ...
    jmx.getJmxClient().readProperty("Alfresco:Name=DatabaseInformation", "DriverName")
  ```
- ability to define your alfresco test environment properties (server, credentials, etc.)  
- ability to query DBs