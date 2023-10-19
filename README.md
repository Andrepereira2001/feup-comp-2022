##GROUP: 8c

André Pereira (201905650), Grade: 19.5, Contribution: 25%

Beatriz Santos (201906888), Grade: 19.5, Contribution: 25%

Matilde Oliveira (201906954), Grade: 19.5, Contribution: 25%

Rodrigo Tuna (201904967), Grade: 19.5, Contribution: 25%

GLOBAL Grade of the project: 19.5

#### SUMMARY
All tasks were implemented, starting by doing Parser of the code to a Tree,
Semantic analysis with errors reports, Ollir generator and Jasmin prodcution. 

There were also made some optimizations clearly identified here: 
- Register Allocation
- Constant Propagation
- Constant Folding
- Copy Propagation
- Dead Code Elimination (eliminating variables that are no longer used, as well as, blocks of code not executed or with no influence to the code after, if there may be a while/if statement with the excution of functions but that do not need to be inside the loop or the condition it is executed outside the basic block)
- Elimination of Unnecessary gotos
- Usage of more efficient JVM Instructions

To test this optimizations we added some tests that are in the Optimize Test Class if you want to check them.

#### SEMANTIC ANALYSIS

We check all semantic errors and rules that the code may have, 
especially the ones indicated to be implemented. 

From them, we want to highlight that we check:
- if every variable used is declared; 
- if every type is valid and if the assignments and functions parameters passed are from that type; 
- if the conditions on while/if statements have a boolean value and; 
- if the operations can be done with the variables. 

Also, just to mention that if we don't know the return type of function we just assume that it is coeherent with the use of the function and does not report any error.

#### CODE GENERATION

We iterate over the final AST and convert it to ollir code. 
Sometimes we have the need to use auxiliary variables to keep the intermediate generated code with a correct flow regarding for example the need to create auxiliar variables before the use of it.  

After the intermediate code we convert it to the Jasmin code following the specified rules and with the properly fitted optimization instructions.

In the Jasmin generation we made use of the `ClassUnit` that represents in a structured way the intermediate code previously generated. 
We made use of said structure to generate code, using a top-down approach (started in the class, then methods and finally instructions).
In order to calculate the stack size two auxiliary variables where used, one that saved the size of the stack at the present moment and
another one that saved the maximum stack size at any given moment, the stack size is of course the second variable after the entire method
was translated.

#### PROS

We fulfilled all objectives of the project. 
The fact that we also covered a great amount of optimizations in the project is one thing we can be proud of. 

#### CONS

We just did not implement two specific optimizations that were: 
- regarding the elimination of unnecessary gotos, we do not evaluate expressions in the conditions, if they are not simplified by the other optimizations implemented. We just optimize if the value of the condition comes to be true. 
- regarding the usage of more efficient JVM instructions, there is one specific instruction that we do not contemplate `iinc` as we felt would unnecessarily complicate the code with not too much of a benefit (only one less variable in the stack).

# Compilers Project

For this project, you need to install [Java](https://jdk.java.net/), [Gradle](https://gradle.org/install/), and [Git](https://git-scm.com/downloads/) (and optionally, a [Git GUI client](https://git-scm.com/downloads/guis), such as TortoiseGit or GitHub Desktop). Please check the [compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html) for Java and Gradle versions.

## Project setup

There are three important subfolders inside the main folder. First, inside the subfolder named ``javacc`` you will find the initial grammar definition. Then, inside the subfolder named ``src`` you will find the entry point of the application. Finally, the subfolder named ``tutorial`` contains code solutions for each step of the tutorial. JavaCC21 will generate code inside the subfolder ``generated``.

## Compile and Running

To compile and install the program, run ``gradle installDist``. This will compile your classes and create a launcher script in the folder ``./build/install/comp2022-00/bin``. For convenience, there are two script files, one for Windows (``comp2022-00.bat``) and another for Linux (``comp2022-00``), in the root folder, that call tihs launcher script.

After compilation, a series of tests will be automatically executed. The build will stop if any test fails. Whenever you want to ignore the tests and build the program anyway, you can call Gradle with the flag ``-x test``.

## Test

To test the program, run ``gradle test``. This will execute the build, and run the JUnit tests in the ``test`` folder. If you want to see output printed during the tests, use the flag ``-i`` (i.e., ``gradle test -i``).
You can also see a test report by opening ``./build/reports/tests/test/index.html``.

## Checkpoint 1
For the first checkpoint the following is required:

1. Convert the provided e-BNF grammar into JavaCC grammar format in a .jj file
2. Resolve grammar conflicts, preferably with lookaheads no greater than 2
3. Include missing information in nodes (i.e. tree annotation). E.g. include the operation type in the operation node.
4. Generate a JSON from the AST

### JavaCC to JSON
To help converting the JavaCC nodes into a JSON format, we included in this project the JmmNode interface, which can be seen in ``src-lib/pt/up/fe/comp/jmm/ast/JmmNode.java``. The idea is for you to use this interface along with the Node class that is automatically generated by JavaCC (which can be seen in ``generated``). Then, one can easily convert the JmmNode into a JSON string by invoking the method JmmNode.toJson().

Please check the JavaCC tutorial to see an example of how the interface can be implemented.

### Reports
We also included in this project the class ``src-lib/pt/up/fe/comp/jmm/report/Report.java``. This class is used to generate important reports, including error and warning messages, but also can be used to include debugging and logging information. E.g. When you want to generate an error, create a new Report with the ``Error`` type and provide the stage in which the error occurred.


### Parser Interface

We have included the interface ``src-lib/pt/up/fe/comp/jmm/parser/JmmParser.java``, which you should implement in a class that has a constructor with no parameters (please check ``src/pt/up/fe/comp/CalculatorParser.java`` for an example). This class will be used to test your parser. The interface has a single method, ``parse``, which receives a String with the code to parse, and returns a JmmParserResult instance. This instance contains the root node of your AST, as well as a List of Report instances that you collected during parsing.

To configure the name of the class that implements the JmmParser interface, use the file ``config.properties``.

### Compilation Stages 

The project is divided in four compilation stages, that you will be developing during the semester. The stages are Parser, Analysis, Optimization and Backend, and for each of these stages there is a corresponding Java interface that you will have to implement (e.g. for the Parser stage, you have to implement the interface JmmParser).


### config.properties

The testing framework, which uses the class TestUtils located in ``src-lib/pt/up/fe/comp``, has methods to test each of the four compilation stages (e.g., ``TestUtils.parse()`` for testing the Parser stage). 

In order for the test class to find your implementations for the stages, it uses the file ``config.properties`` that is in root of your repository. It has four fields, one for each stage (i.e. ``ParserClass``, ``AnalysisClass``, ``OptimizationClass``, ``BackendClass``), and initially it only has one value, ``pt.up.fe.comp.SimpleParser``, associated with the first stage.

During the development of your compiler you will update this file in order to setup the classes that implement each of the compilation stages.
