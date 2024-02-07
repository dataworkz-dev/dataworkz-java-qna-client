# Running the Dataworkz RAG Java QnA Client
Perform the following steps to build the client - 
1. `git clone <TODO>`
2. `cd dataworkz-java-qna-client`
3. `mvn clean package` 

At this point you should have a fat jar in dataworkz-java-qna-client/target named `dataworkz-java-qna-client-1.0-jar-with-dependencies.jar`
 
From inside the `dataworkz-java-qna-client` folder you can run
`java -cp target/dataworkz-java-qna-client-1.0-jar-with-dependencies.jar`

which should generate output like this - 
```
Missing required subcommand
Usage: QnACLIClient [-hV] [COMMAND]
Client to Dataworkz RAG Builder
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  list-systems    List all available QnA Systems
  get-system      Get details of the specified QnA system
  list-llms       List all available LLMProviders in a QnA system
  ask             Ask a question
  list-questions  List all questions in a QnA system
  get-question    Get details of a specific previously asked question in a QnA
                    system
```

The client takes a command and a list of parameters specific to that command. To get help on a command e.g. `list-systems` use `-h` e.g.
```
java -jar target/dataworkz-java-qna-client-1.0-jar-with-dependencies.jar list-systems -h
Usage: QnACLIClient list-systems [-hV] [-cf=<configFile>]
                                 [-ds=<secondsBetweenQueries>] [-f=<format>]
                                 [-if=<inputFile>] [-k=<apiKey>]
                                 [-of=<outputFile>] [-service=<dwHost>]
List all available QnA Systems
      -cf, -config-file=<configFile>
                          Config file. Any other command line parameters
                            override values in config file.
      -ds, -delay-secs=<secondsBetweenQueries>
                          How many seconds to wait between questions?
  -f, -output-format=<format>
                          Format of result. Can be console (default) |
                            console-plain | json
  -h, --help              Show this help message and exit.
      -if, -input-file=<inputFile>
                          Input file of questions to run.
  -k, -api-key=<apiKey>   Dataworkz API Key
      -of, -output-file=<outputFile>
                          Location of output file
      -service=<dwHost>   Dataworkz service to target. e.g. mongodb.dataworkz.io
  -V, --version           Print version information and exit.

```

You will need a Dataworkz API Key for a Dataworkz RAG service to execute the client. 

## Use-Cases
The client can be used 
- As a Java sample application of how to use the Dataworkz RAG Builder API
- As a client to the RAG QnA system to ask questions using either your system or a  Playground system
- To plug-in to a validation/evaluation framework

Besides command line parameters, the client can also be configured via a config file.

## Config File
Some of the common parameters such as `service` and `apiKey` can be provided in a config file. The config file is a Java Properties file with a format - 
```
service=<dw-service-url>
api-key=<dw-api-key>
qa=<qna-system-id>
llm=<llm-id>
delay-secs=5
```
All properties are not necessary in the config file. You can use what is convenient. Parameters required for a command need to available either in the config file or as command line parameters. If both are present, command line parameters override the config file.

## Input File
Command line inputs can be provided in an input file and are executed one after the other with a `delay-secs` delay between subsequent comamnds. 

## Output Formats
By default, the client is configured for a console format with ANSI colors. Other formats include - 
- `none` : No output
- `console` : Default - colored console output where supported
- `console-plain` : Console output without ANSI colors
- `json` : Json format as received by the API response

## Output File
Output can be written to a specified file

## MongoDB Partner Playground
To use the MongoDB Partner Playground at `https://mongodb.dataworkz.com` [create an api key](https://docs.dataworkz.com/product-docs/api-key-generation/generate-api-key-in-dataworkz) and provide the service in the `-service` parameter and the api-key in the `-k` parameter or provide them in a config file.  

## Issues
Please report any bugs in Issues.