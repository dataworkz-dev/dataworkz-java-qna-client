package com.dataworkz.qna.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import picocli.CommandLine;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.dataworkz.qna.client.QnACLIClient.*;

@CommandLine.Command(name = "QnACLIClient", mixinStandardHelpOptions = true, version = "qna-client 1.0",
        description = "Client to Dataworkz RAG Builder",
        subcommands = {
            DoListSystemsCommand.class,
            DoGetSystemCommand.class,
            DoListLLMsCommand.class,
            DoQuestionCommand.class,
            DoListQuestionsCommand.class,
            DoGetQuestionCommand.class,
            DoSemanticSearchCommand.class
        }
)
public class QnACLIClient {
    static final String KEY_FORMAT = "bold,green";
    static final String VALUE_FORMAT = "white";
    static final String FAIL_FORMAT = "bold,red";
    private static final Map<String, BaseQnAClient> commands = Map.of(
            "question", new DoQuestionCommand(),
            "list-systems", new DoListSystemsCommand(),
            "get-system", new DoGetSystemCommand(),
            "list-llms", new DoListLLMsCommand(),
            "list-questions", new DoListQuestionsCommand(),
            "get-questions", new DoGetQuestionCommand(),
            "search", new DoSemanticSearchCommand()
    );


    public static void main(String[] args) {
        int exitCode = 0;
        try {
            CommandLine commandLine = new CommandLine(new QnACLIClient());
            exitCode = commandLine.execute(args);
        } catch (Exception ex) {
            System.err.println("Failed to execute : " + ex.getMessage());
            ex.printStackTrace();
        }
        System.exit(exitCode);
    }
}

@CommandLine.Command(name="ask", mixinStandardHelpOptions = true, description = "Ask a question")
class DoQuestionCommand extends BaseQnAClient implements Callable<Integer> {
    @CommandLine.Option(names = {"-llm"}, description = "Id of LLM to target. Multiple may be provided separated by ;")
    private String llmId;
    @CommandLine.Option(names = {"-qa", "-system"}, description = "Id of QnA system to target.")
    private String qnaSystemId;
    @CommandLine.Option(names = {"-q", "-question"}, description = "Question Text")
    private String questionText;
    @CommandLine.Option(names = {"-ft", "-filter"}, description = "Filter String")
    private String filterString;

    @CommandLine.Option(names = {"-qp", "-query-plan"}, description = "Query Plan")
    private String queryPlan;

    @CommandLine.Option(names = {"-p", "-probe"}, description = "Display probe data")
    private boolean showProbeData;

    @CommandLine.Option(names = {"-ps", "-properties"}, description = "Pass properties")
    private String properties;

    @Override
    protected void loadOptions() {
        qnaSystemId = getOptionValue(qnaSystemId, "qa");
        llmId = getOptionValue(llmId, "llm");
        if (!isOptionPresent(qnaSystemId)) {
            throw new IllegalArgumentException("A QnA system must be specified using the -qa option");
        }
        if (!isOptionPresent(llmId)) {
            throw new IllegalArgumentException("An LLM Id must be provided -llm option");
        }
        if (!isOptionPresent(questionText) && !isOptionPresent(inputFile)) {
            throw new IllegalArgumentException("Ask your question using the -q option");
        }
    }

    @Override
    protected void loadInputFromInputFile(int index, String input) {
        this.questionText = input;
    }

    @Override
    protected RAGResponse doCallImpl(DataworkzRAG dw) throws URISyntaxException, IOException, InterruptedException {
        return dw.askQuestion(qnaSystemId, llmId, questionText, filterString, queryPlan, properties);
    }

    @Override
    void printMapResponse(String indent, StringBuilder output, Map<String, ?> payload, EntryRenderer<String, ?> entryRenderer) {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("question", String.valueOf(payload.get("question")));
        out.put("answer", String.valueOf(payload.get("answer")));
        List<Map<String, Object>> context = (List<Map<String, Object>>) payload.get("context");
        List<String> links = context.stream().map(cMap -> String.valueOf(cMap.get("link"))).collect(Collectors.toList());
        out.put("links", links.toString());
        Set<String> skipKeys = new HashSet<>(Set.of("question", "answer", "context"));
        if (!showProbeData) {
            skipKeys.add("probe");
        }
        payload.forEach((s, o) -> {
            if (!skipKeys.contains(s)) {
                out.put(s, String.valueOf(o));
            }
        });
        if (!showProbeData) {
            out.put("probe", "Use -p to show probe data");
        }
        super.printMapResponse(indent, output, out, entryRenderer);
    }
}

@CommandLine.Command(name="list-systems", mixinStandardHelpOptions = true, description = "List all available QnA Systems")
class DoListSystemsCommand extends BaseQnAClient implements Callable<Integer> {
    @Override
    protected void loadOptions() {

    }

    @Override
    protected RAGResponse doCallImpl(DataworkzRAG dw) throws IOException, URISyntaxException, InterruptedException {
        return dw.listQnASystems();
    }
}

@CommandLine.Command(name="list-llms", mixinStandardHelpOptions = true, description = "List all available LLMProviders in a QnA system")
class DoListLLMsCommand extends BaseQnAClient implements Callable<Integer> {
    @CommandLine.Option(names = {"-qa", "-system"}, description = "Id of QnA system to target.")
    private String qnaSystemId;

    @Override
    protected void loadOptions() {
        qnaSystemId = getOptionValue(qnaSystemId, "qa");
        if (!isOptionPresent(qnaSystemId)) {
            throw new IllegalArgumentException("qnaSystemId is required using the -qa option.");
        }
    }

    @Override
    protected RAGResponse doCallImpl(DataworkzRAG dw) throws URISyntaxException, IOException, InterruptedException {
        return dw.listLLMs(qnaSystemId);
    }
}

@CommandLine.Command(name="list-questions", mixinStandardHelpOptions = true, description = "List all questions in a QnA system")
class DoListQuestionsCommand extends BaseQnAClient implements Callable<Integer> {
    @CommandLine.Option(names = {"-qa", "-system"}, description = "Id of QnA system to target.")
    private String qnaSystemId;

    @Override
    protected void loadOptions() {
        qnaSystemId = getOptionValue(qnaSystemId,"qa");
        if (!isOptionPresent(qnaSystemId)) {
            throw new IllegalArgumentException("qnaSystemId is required using the -qa option.");
        }
    }

    @Override
    protected RAGResponse doCallImpl(DataworkzRAG dw) throws URISyntaxException, IOException, InterruptedException {
        return dw.listQuestions(qnaSystemId);
    }

    @Override
    String getResponseAsString(RAGResponse response) {
        return printMapResponse("", response, new EntryRenderer<>() {
            @Override
            void render(Map.Entry<String, ?> e, String indent, StringBuilder sb) {
                Map<String, String> value = (Map<String, String>) e.getValue();
                sb.append(CommandLine.Help.Ansi.AUTO.string("\n@|" + KEY_FORMAT + " " + e.getKey() + "|@ : "));
                printMapResponse(indent + "\t", sb, value, null);
                //super.render(e, indent, sb);
            }
        });
    }
}

@CommandLine.Command(name="get-system", mixinStandardHelpOptions = true, description = "Get details of the specified QnA system")
class DoGetSystemCommand extends BaseQnAClient implements Callable<Integer> {
    @CommandLine.Option(names = {"-qa", "-system"}, description = "Id of QnA system to target.")
    private String qnaSystemId;

    @Override
    protected void loadOptions() {
        qnaSystemId = getOptionValue(qnaSystemId, "qa");
        if (!isOptionPresent(qnaSystemId) && !isOptionPresent(inputFile)) {
            throw new IllegalArgumentException("qnaSystemId is required using the -qa option.");
        }
    }

    @Override
    protected void loadInputFromInputFile(int index, String input) {
        this.qnaSystemId = input;
    }

    @Override
    protected RAGResponse doCallImpl(DataworkzRAG dw) throws URISyntaxException, IOException, InterruptedException {
        return dw.getSystem(qnaSystemId);
    }

    @Override
    void printMapResponse(String indent, StringBuilder output, Map<String, ?> payload, EntryRenderer<String, ?> entryRenderer) {
        Map<String, String> out = new HashMap<>();
        payload.forEach((s, o) -> {
            if (!s.equals("params")) {
                out.put(s, String.valueOf(o));
            }
        });
        super.printMapResponse(indent, output, out, entryRenderer);
        output.append(CommandLine.Help.Ansi.AUTO.string("\n@|" + KEY_FORMAT + " params |@ : "));
        Map<String, Object> params = (Map<String, Object>) payload.get("params");
        super.printMapResponse(indent + "\t", output, params, entryRenderer);
    }
}

@CommandLine.Command(name="get-question", mixinStandardHelpOptions = true, description = "Get details of a specific previously asked question in a QnA system")
class DoGetQuestionCommand extends BaseQnAClient implements Callable<Integer> {
    @CommandLine.Option(names = {"-qa", "-system"}, description = "Id of QnA system to target.")
    private String qnaSystemId;
    @CommandLine.Option(names = {"-qId", "-questionId"}, description = "Id of question")
    private String questionId;

    @CommandLine.Option(names = {"-p", "-probe"}, description = "Show probe data")
    private boolean showProbeData;

    @Override
    protected void loadOptions() {
        qnaSystemId = getOptionValue(qnaSystemId, "qa");
        if (!isOptionPresent(questionId) && !isOptionPresent(inputFile)) {
            throw new IllegalArgumentException("QuestionId must be specified using -qid option");
        }
    }

    @Override
    protected void loadInputFromInputFile(int index, String input) {
        this.questionId = input;
    }

    @Override
    protected RAGResponse doCallImpl(DataworkzRAG dw) throws URISyntaxException, IOException, InterruptedException {
        return dw.getQuestion(qnaSystemId, questionId);
    }

    @Override
    void printMapResponse(String indent, StringBuilder output, Map<String, ?> payload, EntryRenderer<String, ?> entryRenderer) {
        String llmResponse = (String) payload.get("llm_response");
        if (llmResponse == null) {
            throw new IllegalStateException("Unexpected response");
        }
        Gson gson = new Gson();
        LinkedHashMap<String, Object> llmResponseObj = gson.fromJson(llmResponse, new TypeToken<>() {});
        List<Map<String, String>> context = (List<Map<String, String>>) llmResponseObj.get("context");
        List<String> links = context.stream().map(cMap -> cMap.get("link")).collect(Collectors.toList());
        llmResponseObj.put("links", links.toString());
        if (!showProbeData) {
            llmResponseObj.put("probe", "Use -p to show probe data");
        }
        output.append(CommandLine.Help.Ansi.AUTO.string("\n@|" + KEY_FORMAT + " llm_response |@ : "));
        super.printMapResponse(indent + "\t", output, llmResponseObj, entryRenderer);
        Map<String, Object> rest = new HashMap<>(payload);
        rest.remove("llm_response");
        super.printMapResponse(indent, output, rest, entryRenderer);
    }
}

@CommandLine.Command(name="search", mixinStandardHelpOptions = true, description = "Do Seamntic Search of the query in a QnA system")
class DoSemanticSearchCommand extends BaseQnAClient implements Callable<Integer> {
    @CommandLine.Option(names = {"-qa", "-system"}, description = "Id of QnA system to target.")
    private String qnaSystemId;
    @CommandLine.Option(names = {"-q", "-question"}, description = "Question Text")
    private String questionText;

    @CommandLine.Option(names = {"-ft", "-filter"}, description = "Filter String")
    private String filter;

    @CommandLine.Option(names = {"-qp", "-query-plan"}, description = "Query Plan")
    private String queryPlan;

    @CommandLine.Option(names = {"-llm"}, description = "Id of LLM to target. Multiple may be provided separated by ;")
    private String llmId;

    @CommandLine.Option(names = {"-p", "-probe"}, description = "Display probe data")
    private boolean showProbeData;

    @CommandLine.Option(names = {"-ps", "-properties"}, description = "Pass properties")
    private String properties;

    @Override
    protected void loadOptions() {
        qnaSystemId = getOptionValue(qnaSystemId, "qa");
        if (!isOptionPresent(qnaSystemId)) {
            throw new IllegalArgumentException("A QnA system must be specified using the -qa option");
        }
        if (!isOptionPresent(questionText) && !isOptionPresent(inputFile)) {
            throw new IllegalArgumentException("Ask your question using the -q option");
        }
    }

    @Override
    protected void loadInputFromInputFile(int index, String input) {
        this.questionText = input;
    }

    @Override
    protected RAGResponse doCallImpl(DataworkzRAG dw) throws URISyntaxException, IOException, InterruptedException {
        return dw.search(qnaSystemId, questionText, filter, queryPlan, properties);
    }

    @Override
    void printMapResponse(String indent, StringBuilder output, Map<String, ?> payload, EntryRenderer<String, ?> entryRenderer) {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("question", String.valueOf(payload.get("question")));
        Set<String> skipKeys = new HashSet<>(Set.of("query", "searchResultsList", "type"));
        if (!showProbeData) {
            skipKeys.add("probe");
        }
        payload.forEach((s, o) -> {
            if (!skipKeys.contains(s)) {
                out.put(s, String.valueOf(o));
            }
        });
        super.printMapResponse(indent, output, out, entryRenderer);
        String rindent = indent + "\t";
        output.append(CommandLine.Help.Ansi.AUTO.string("\n@|" + KEY_FORMAT + " searchResultsList |@ : "));
        List<Map<String, Object>> context = (List<Map<String, Object>>) payload.get("searchResultsList");
        int i = 1;
        for (Map<String, Object> map : context) {
            String link = String.valueOf(map.get("link"));
            String score = String.valueOf(map.get("similarityScore"));
            String text = String.valueOf(map.get("contents"));  // TODO : needs change when api object is made same

            output.append(CommandLine.Help.Ansi.AUTO.string("\n\n" + indent + "@|" + KEY_FORMAT + " Result " + i++ + " |@ : "));
            output.append(CommandLine.Help.Ansi.AUTO.string(
                    "@|" + KEY_FORMAT + " \n" + rindent + "link"
                            + "|@ : @|" + VALUE_FORMAT + " " + link + "|@"));
            output.append(CommandLine.Help.Ansi.AUTO.string(
                    "@|" + KEY_FORMAT + " \n" + rindent + "score"
                            + "|@ : @|" + VALUE_FORMAT + " " + score + "|@"));
            output.append(CommandLine.Help.Ansi.AUTO.string(
                    "@|" + KEY_FORMAT + " \n" + rindent + "text"
                            + "|@ : @|" + VALUE_FORMAT + " " + text + "|@"));
        }
        if (!showProbeData) {
            out.put("probe", "Use -p to show probe data");
        }
    }
}

abstract class BaseQnAClient implements Callable<Integer> {
    @CommandLine.Option(names = {"-ds", "-delay-secs"}, description = "How many seconds to wait between questions?", defaultValue = "1")
    int secondsBetweenQueries;
    @CommandLine.Option(names = {"-cf", "-config-file"}, description = "Config file. Any other command line parameters override values in config file.")
    String configFile;
    @CommandLine.Option(names = {"-f", "-output-format"}, description = "Format of result. Can be console (default) | console-plain | json",
            defaultValue = "console")
    String format;
    @CommandLine.Option(names = {"-of", "-output-file"}, description = "Location of output file")
    String outputFile;
    @CommandLine.Option(names = {"-service"}, description = "Dataworkz service to target. e.g. mongodb.dataworkz.io")
    String dwHost;
    @CommandLine.Option(names = {"-k", "-api-key"}, description = "Dataworkz API Key")
    String apiKey;
    @CommandLine.Option(names = {"-if", "-input-file"}, description = "Input file of questions to run.")
    String inputFile;

    Properties configProps = new Properties();
    private DataworkzRAG dw;

    private void loadCommonOptions() {
        if (format.equals("console")) {
            System.setProperty("picocli.ansi", "tty");
        } else if (format.equals("console-plain")) {
            System.setProperty("picocli.ansi", "false");
        }

        doIfOptionPresent(configFile, () -> {
            File f = new File(configFile);
            if (!f.exists() || !f.canRead()) {
                throw new IllegalArgumentException("Config File does not exist or cannot be read : " + f.getAbsolutePath());
            }
            try {
                configProps.load(new FileReader(f));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        BiFunction<String, String, String> safeSet = (arg, config) -> arg == null ? config : arg;

        this.dwHost = safeSet.apply(dwHost, configProps.getProperty("service"));
        this.apiKey = safeSet.apply(apiKey, configProps.getProperty("api-key"));

        this.dw = new DataworkzRAG(dwHost, apiKey);

        if (configProps.getProperty("delay-secs") != null && secondsBetweenQueries == 1) { // if it is default
            secondsBetweenQueries = Integer.parseInt(configProps.getProperty("delay-secs"));
        }

        doIfOptionPresent(inputFile, () -> {
            File inf = new File(inputFile);
            if (!inf.exists() || !inf.canRead()) {
                throw new IllegalArgumentException("Input File does not exist or cannot be read : " + inf.getAbsolutePath());
            }
        });

        doIfOptionPresent(outputFile, () -> {
            try {
                File outf = new File(outputFile);
                if (!outf.exists()) {
                    outf.getParentFile().mkdirs();
                    outf.createNewFile();
                }
                if (!outf.exists() || !outf.canWrite()) {
                    throw new IllegalArgumentException("Output File cannot be written : " + outf.getAbsolutePath());
                }

                if (outf.exists()) {
                    // empty out the output file because we might do APPEND later
                    Files.writeString(outf.toPath(), "", StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Integer call() throws Exception {
        loadCommonOptions();
        loadOptions();
        if (inputFile != null) {
            File inf = new File(inputFile);
            List<String> inputs = Files.readAllLines(inf.toPath());
            long count = inputs.stream().filter(s -> !s.startsWith("#")).count();
            System.out.println("Running " + count + " commands. Delay between commands : " + secondsBetweenQueries + " secs");
            int i = 1;
            if (format.equals("json")) {
                doIfOptionPresent(outputFile, () -> {
                    writeToOutput("[\n");
                });
            }
            for (int idx = 0; idx < inputs.size(); idx++) {
                String s = inputs.get(idx);
                if (s.startsWith("# ")) {
                    System.out.printf(s);
                    continue;
                }
                System.out.print("Running command " + i++ + "/" + inputs.size() + " :: " + s);
                long time = System.currentTimeMillis();
                loadInputFromInputFile(i - 2, s);
                RAGResponse response = doCallImpl(dw);
                System.out.println("... Done. Took " + (System.currentTimeMillis() - time) + " msecs");
                outputResponse(response);
                if (format.equals("json")) {
                    if (idx + 1 < inputs.size()) {
                        doIfOptionPresent(outputFile, () -> {
                            writeToOutput(",\n");
                        });
                    }
                }
                try {
                    Thread.sleep(secondsBetweenQueries * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (format.equals("json")) {
                doIfOptionPresent(outputFile, () -> {
                    writeToOutput("]\n");
                });
            }
        } else {
            RAGResponse response = doCallImpl(this.dw);
            outputResponse(response);
        }
        return 0;
    }

    protected void loadInputFromInputFile(int index, String input) {

    }

    protected String getOptionValue(String existing, String configKey) {
        return (existing == null) ? configProps.getProperty(configKey) : existing;
    }

    protected abstract void loadOptions();

    protected abstract RAGResponse doCallImpl(DataworkzRAG dataworkzRAG) throws URISyntaxException, IOException, InterruptedException;

    void doIfOptionPresent(String option, Runnable fn) {
        if (isOptionPresent(option)) {
            fn.run();
        }
    }

    boolean isOptionPresent(String option) {
        return option != null && !option.isEmpty();
    }

    String getResponseAsString(RAGResponse response) {
        return printMapResponse("", response, null);
    }

    String printMapResponse(String indent, RAGResponse response, EntryRenderer<String, ?> entryRenderer) {
        if (response.hasPayload()) {
            StringBuilder output = new StringBuilder();
            Map<String, ?> payload = response.getPayload();
            printMapResponse(indent, output, payload, entryRenderer);
            return output.toString();
        } else {
            return CommandLine.Help.Ansi.AUTO.string("@|" + FAIL_FORMAT+ " Failed: " + response.getResponse() + "|@");
        }
    }

    void printMapResponse(String indent, StringBuilder output, Map<String, ?> payload, EntryRenderer<String, ?> entryRenderer) {
        EntryRenderer<String, ?> renderer = entryRenderer == null ? new EntryRenderer<>() : entryRenderer;
        payload.entrySet().forEach(e -> renderer.render(e, indent, output));
    }

    protected void writeToOutput(String text) {
        if (isOptionPresent(outputFile)) {
            try {
                Files.writeString(new File(outputFile).toPath(), text, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println(text);
        }
    }

    private void outputResponse(RAGResponse response) {
        String output = "";
        if (format.equals("none")) {
            return;
        } else if (format.startsWith("console")) {
            output = getResponseAsString(response);
        } else if (format.equals("json")) {
            output = getBodyString(response);
        } else {
            throw new IllegalArgumentException("Invalid format value " + format);
        }
        writeToOutput(output);
//        if (isOptionPresent(outputFile)) {
////            try {
////                Files.writeString(new File(outputFile).toPath(), getBodyString(response), StandardOpenOption.APPEND);
////            } catch (IOException e) {
////                throw new RuntimeException(e);
////            }
//        } else {
//            System.out.println(output);
//        }
    }

    private static String getBodyString(RAGResponse response) {
        return String.valueOf(response.getResponse().body());
//        return body.endsWith("\n") ? body : body + "\n";
    }

    protected static class EntryRenderer<String, V> {
        void render(Map.Entry<String, ?> e, String indent, StringBuilder sb) {
            sb.append(CommandLine.Help.Ansi.AUTO.string(
                    "@|" + KEY_FORMAT + " \n" + indent + e.getKey()
                            + "|@ : @|" + VALUE_FORMAT + " " + e.getValue() + "|@"));
        }
    }
}
