package org.alfresco.utility.testrail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.DatatypeConverter;

import org.alfresco.utility.Utility;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.alfresco.utility.testrail.model.Run;
import org.alfresco.utility.testrail.model.Section;
import org.alfresco.utility.testrail.model.TestCase;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Basic implementation of interacting with Test Rail
 * 
 * @author Paul Brodner
 */
public class TestRailApi
{
    static Logger LOG = LoggerFactory.getLogger("testrail");

    /*
     * Test Rail Template:
     * 1 - Test Case
     */
    private static final int TEMPLATE_ID = Integer.valueOf(1);
    private static final int TEST_PRIORITY_MEDIUM = 2;

    Properties testRailProperties = new Properties();

    private String username;
    private String password;
    private String endPointApiPath;
    public int currentProjectID;
    private String currentRun;
    private boolean configurationError = true;
    public int suiteId;
    protected String serverUrl;

    private TestCase tmpTestCase = null;

    public TestCase getCurrentTestCase()
    {
        return tmpTestCase;
    }

    public void setCurrentTestCase(TestCase testCase)
    {
        this.tmpTestCase = testCase;
    }

    /**
     * Setup configuration from property file
     */
    public TestRailApi()
    {
        InputStream defaultPropsInputStream = getClass().getClassLoader().getResourceAsStream(Utility.getEnvironmentPropertyFile());
        if (defaultPropsInputStream != null)
        {
            try
            {
                testRailProperties.load(defaultPropsInputStream);

                this.username = Utility.getSystemOrFileProperty("testManagement.username", testRailProperties);
                Utility.checkObjectIsInitialized(username, "username");

                this.password = Utility.getSystemOrFileProperty("testManagement.apiKey", testRailProperties);
                Utility.checkObjectIsInitialized(password, "password");

                this.endPointApiPath = Utility.getSystemOrFileProperty("testManagement.endPoint", testRailProperties) + "index.php?/api/v2/";
                Utility.checkObjectIsInitialized(endPointApiPath, "endPointApiPath");

                this.currentProjectID = Integer.parseInt(Utility.getSystemOrFileProperty("testManagement.project", testRailProperties));
                Utility.checkObjectIsInitialized(currentProjectID, "currentProjectID");

                this.currentRun = Utility.getSystemOrFileProperty("testManagement.testRun", testRailProperties);
                Utility.checkObjectIsInitialized(currentRun, "currentRun");

                this.suiteId = Integer.valueOf(Utility.getSystemOrFileProperty("testManagement.suiteId", testRailProperties));
                Utility.checkObjectIsInitialized(suiteId, "suiteId");

                /*
                 * alfresco.scheme=http
                 * alfresco.server=localhost
                 * alfresco.port=8080
                 */
                serverUrl = String.format("%s://%s:%s", testRailProperties.getProperty("alfresco.scheme"), testRailProperties.getProperty("alfresco.server"),
                        testRailProperties.getProperty("alfresco.port"));
                configurationError = false;
            }
            catch (Exception e)
            {
                LOG.error("Cannot initialize Test Management Setting from default.properties file");
            }
        }
        else
        {
            LOG.error("Cannot initialize Test Management Setting from {} file", Utility.getEnvironmentPropertyFile());
        }
    }

    protected <T> T toClass(Object response, Class<T> classz)
    {
        ObjectMapper mapper = new ObjectMapper();

        try
        {
            return mapper.readValue(response.toString(), classz);
        }
        catch (JsonParseException e)
        {
            LOG.error(e.getMessage());
        }
        catch (JsonMappingException e)
        {
            LOG.error(e.getMessage());
        }
        catch (IOException e)
        {
            LOG.error(e.getMessage());
        }
        return null;
    }

    /**
     * Parse the response received and convert it to <classz> passed as parameter
     * 
     * @param response
     * @param classz
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <T> List<T> toCollection(Object response, Class<T> classz)
    {
        ObjectMapper mapper = new ObjectMapper();
        List<Section> list = null;
        try
        {
            list = mapper.readValue(response.toString(), TypeFactory.defaultInstance().constructCollectionType(List.class, classz));
        }
        catch (JsonParseException e)
        {
            LOG.error(e.getMessage());
        }
        catch (JsonMappingException e)
        {
            LOG.error(e.getMessage());
        }
        catch (IOException e)
        {
            LOG.error(e.getMessage());
        }
        return (List<T>) list;
    }

    public boolean hasConfigurationErrors()
    {
        return configurationError;
    }

    protected Object getRequest(String path) throws Exception
    {
        URL endPointURL = new URL(endPointApiPath + path);
        HttpURLConnection conn = (HttpURLConnection) endPointURL.openConnection();
        conn.addRequestProperty("Content-Type", "application/json");

        conn.addRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary(String.format("%s:%s", username, password).getBytes()));

        return parseRespose(conn);
    }

    protected Object postRequest(String path, Object data) throws Exception
    {
        URL endPointURL = new URL(endPointApiPath + path);
        HttpURLConnection conn = (HttpURLConnection) endPointURL.openConnection();
        conn.addRequestProperty("Content-Type", "application/json");

        conn.addRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary(String.format("%s:%s", username, password).getBytes()));
        if (data != null)
        {
            byte[] block = JSONValue.toJSONString(data).getBytes("UTF-8");
            conn.setDoOutput(true);
            OutputStream ostream = conn.getOutputStream();
            ostream.write(block);
            ostream.flush();
        }

        return parseRespose(conn);
    }

    /**
     * Parse the response returned.
     * 
     * @param conn
     * @return
     * @throws Exception
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    private Object parseRespose(HttpURLConnection conn) throws Exception, IOException, UnsupportedEncodingException
    {
        int status = conn.getResponseCode();
        InputStream istream;
        if (status != 200)
        {
            istream = conn.getErrorStream();
            if (istream == null)
            {
                throw new Exception("TestRail API return HTTP " + status + " (No additional error message received)");
            }
        }
        else
        {
            istream = conn.getInputStream();
        }

        String text = "";
        if (istream != null)
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(istream, "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null)
            {
                text += line;
                text += System.getProperty("line.separator");
            }
            reader.close();
        }

        Object result;
        if (text != "")
        {
            result = JSONValue.parse(text);
        }
        else
        {
            result = new JSONObject();
        }

        if (status != 200)
        {
            String error = "No additional error message received";
            if (result != null && result instanceof JSONObject)
            {
                JSONObject obj = (JSONObject) result;
                if (obj.containsKey("error"))
                {
                    error = '"' + (String) obj.get("error") + '"';
                }
            }

            throw new Exception("TestRail API returned HTTP " + status + "(" + error + ")");
        }
        return result;
    }

    public List<Section> getSectionsOfCurrentProject()
    {
        return getSections(currentProjectID);
    }

    @SuppressWarnings("unchecked")
    public Section addNewSection(String name, int parent_id, int projectID, int suite_id)
    {
        Section s = new Section();

        @SuppressWarnings("rawtypes")
        Map data = new HashMap();
        data.put("suite_id", suite_id);
        data.put("name", name);
        data.put("parent_id", parent_id);

        LOG.info("Add missing section [{}] as child of parent section with ID: {}", name, parent_id);
        Object response;
        try
        {
            response = postRequest("add_section/" + projectID, data);
            s = toClass(response, Section.class);
        }
        catch (Exception e)
        {
            LOG.error("Cannot add new section: {}", e.getMessage());
        }
        return s;
    }

    public Run getRun(String name, int projectID)
    {
        for (Run run : getRuns(projectID))
        {
            if (run.getName().equals(name) && !run.isIs_completed())
                return run;
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addTestCase(ITestResult result, Section section, TestRail annotation)
    {
        try
        {
            Map data = new HashMap();
            data.put("title", result.getMethod().getMethodName());
            data.put("template_id", TEMPLATE_ID);
            data.put("type_id", annotation.testType().value());

            // /*
            // * Steps
            // */
            // @SuppressWarnings("rawtypes")
            // List steps = new ArrayList();
            //
            // @SuppressWarnings("rawtypes")
            // Map step1 = new HashMap();
            // step1.put("status_id", new Integer(1));
            // step1.put("content", "Step 1");
            // step1.put("expected", "desc2");
            // steps.add(step1);
            // data.put("custom_steps_separated", steps);
            // data.put("custom_steps", annotation.description());
            data.put("custom_auto_ref", getFullTestCaseName(result));
            data.put("custom_executiontype", Boolean.valueOf(true)); // always automated

            // holds Sanity, Smoke, Regression, etc
            List<Integer> executionTypeList = new ArrayList<Integer>();
            for (ExecutionType et : annotation.executionType())
            {
                executionTypeList.add(et.value());
            }

            data.put("custom_exce_type", executionTypeList);
            data.put("custom_description", annotation.description());
            data.put("priority_id", Integer.valueOf(TEST_PRIORITY_MEDIUM));
            data.put("custom_platform", 1);

            Object response = postRequest("add_case/" + section.getId(), data);
            setCurrentTestCase(toClass(response, TestCase.class));
        }
        catch (Exception e)
        {
            LOG.error(e.getMessage());
        }
    }

    public void addTestSteps(ITestResult result, String steps, Section section, TestRail annotation)
    {
        try
        {
            Map<String, String> data = new HashMap<String, String>();
            data.put("custom_test_notes", steps);

            if (isAutomatedTestCaseInSection(result.getMethod().getMethodName(), section, annotation))
            {
                Object response = postRequest("update_case/" + tmpTestCase.getId(), data);
                setCurrentTestCase(toClass(response, TestCase.class));
            }
        }
        catch (Exception e)
        {
            LOG.error(e.getMessage());
        }
    }

    public boolean isAutomatedTestCaseInSection(String testName, Section section, TestRail annotation)
    {
        /* index.php?/api/v2/get_cases/1&section_id=2&type_id=<custom> */
        tmpTestCase = null;
        try
        {
            Object response = getRequest(
                    "/get_cases/" + currentProjectID + "&type_id=" + annotation.testType().value() + "&suite_id=" + suiteId + "&section_id=" + section.getId());
            List<TestCase> existingTestCases = toCollection(response, TestCase.class);
            for (TestCase tc : existingTestCases)
            {
                if (tc.getTitle().equals(testName))
                {
                    setCurrentTestCase(tc);
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            LOG.error(String.format("Cannot get test cases from Test Rail. Error %s", e.getMessage()));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public Object updateTestCaseResult(ITestResult result, Run run)
    {
        if (tmpTestCase == null)
            return null;
        int status = 2; // blocked in Test Rail

        switch (result.getStatus())
        {
            case ITestResult.SUCCESS:
                status = 1; // Passed in Test Rail
                break;
            case ITestResult.FAILURE:
                status = 5; // Failed in Test Rail
                break;
            case ITestResult.SKIP:
                status = 4; // Retest in TestRail
                break;
            case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
                status = 1;
                break;
            default:
                break;
        }

        @SuppressWarnings("rawtypes")
        Map data = new HashMap();
        data.put("status_id", status);

        /*
         * adding stack trace of failed test
         */
        if (result.getThrowable() != null)
        {
            if (result.getThrowable().getStackTrace() != null)
            {
                StringWriter sw = new StringWriter();
                result.getThrowable().printStackTrace(new PrintWriter(sw));
                data.put("comment", sw.toString());
            }
        }

        /*
         * BUG section, taking in consideration TestNG tests that are marked with @Bug annotation
         */
        Bug bugAnnotated = result.getMethod().getConstructorOrMethod().getMethod().getAnnotation(Bug.class);

        if (bugAnnotated != null)
        {
            data.put("defects", bugAnnotated.id());
        }

        Object response = "";
        try
        {
            response = postRequest("add_result_for_case/" + run.getId() + "/" + tmpTestCase.getId(), data);
        }
        catch (Exception e)
        {
            LOG.error("Cannot update Test Case status execution. Error: {}, Response: {}", e.getMessage(), response.toString());
            return e.getMessage();
        }

        return response;
    }

    /**
     * Returns the current Test Runs of current project
     */
    @SuppressWarnings("unchecked")
    public Run getRunOfCurrentProject()
    {
        Run r = getRun(currentRun, currentProjectID);
        if (r == null)
        {
            @SuppressWarnings("rawtypes")
            Map data = new HashMap();
            data.put("suite_id", suiteId);
            data.put("name", currentRun);
            data.put("include_all", true);
            data.put("description", "**Server:** " + serverUrl);

            LOG.info("Add new RUN [{}]", currentRun);
            Object response;
            try
            {
                response = postRequest("add_run/" + currentProjectID, data);
                r = toClass(response, Run.class);
            }
            catch (Exception e)
            {
                LOG.error("Cannot add new section: {}", e.getMessage());
            }
        }

        return r;
    }

    public String getFullTestCaseName(ITestResult result)
    {
        return String.format("%s#%s", result.getInstanceName(), result.getMethod().getMethodName());
    }

    public List<Section> getSections(int projectID)
    {
        LOG.info("Get all sections from Test Rail Project with id: {}", projectID);
        Object response;
        try
        {
            response = getRequest("get_sections/" + projectID + "&suite_id=" + suiteId);
            return toCollection(response, Section.class);
        }
        catch (Exception e)
        {
            LOG.error(e.getMessage());
        }
        return new ArrayList<Section>();
    }

    public List<Run> getRuns(int projectID)
    {
        Object response;
        LOG.info("Get all Runs from Test Rail Project with id: {}", projectID);
        try
        {
            response = getRequest("get_runs/" + projectID);
            return toCollection(response, Run.class);
        }
        catch (Exception e)
        {
            LOG.error(e.getMessage());
        }
        return new ArrayList<Run>();
    }
}
