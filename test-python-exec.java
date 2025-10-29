import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sensorvision.model.FunctionRuntime;
import org.sensorvision.model.Organization;
import org.sensorvision.model.ServerlessFunction;
import org.sensorvision.service.functions.FunctionExecutionResult;
import org.sensorvision.service.functions.PythonFunctionExecutor;

public class TestPythonExec {
    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        PythonFunctionExecutor executor = new PythonFunctionExecutor(objectMapper);

        String pythonCode = """
            def main(event):
                return {"result": "Hello from Python", "input": event}
            """;

        Organization org = new Organization();
        org.setId(1L);
        org.setName("Test Org");

        ServerlessFunction function = new ServerlessFunction();
        function.setId(1L);
        function.setOrganization(org);
        function.setName("test-function");
        function.setRuntime(FunctionRuntime.PYTHON_3_11);
        function.setCode(pythonCode);
        function.setHandler("main");
        function.setTimeoutSeconds(30);
        function.setMemoryLimitMb(512);
        function.setEnabled(true);

        JsonNode input = objectMapper.readTree("{\"value\": 42}");

        FunctionExecutionResult result = executor.execute(function, input);

        System.out.println("Success: " + result.isSuccess());
        System.out.println("Output: " + result.getOutput());
        System.out.println("Error Message: " + result.getErrorMessage());
        System.out.println("Error Stack: " + result.getErrorStack());
        System.out.println("Duration: " + result.getDurationMs() + "ms");
    }
}
