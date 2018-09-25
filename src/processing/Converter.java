package processing;

import JSON.JSONArray;
import JSON.JSONDictionary;
import checker.ClassVisitor;
import checker.StaticCastVisitor;
import grammar.antlr.CPP14Lexer;
import grammar.antlr.CPP14Parser;
import org.antlr.v4.runtime.*;
import weakclass.CppClass;
import weakclass.CppFunction;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


public class Converter {
    static final String tag = "/* This File is Converted */";

    private HashSet<String> moduleSet = new HashSet<>();
    private String filePath;
    private boolean hasClass;
    private boolean hasStaticCast;
    private Set<CppClass> classSet;

    Converter(String filePath) {
        this.filePath = filePath;
    }

    private boolean checkFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;

            while ((line = br.readLine()) != null) {
                if (line.contains(tag)) {
                    return false;
                }
                if (line.contains("#include")) {
                    if (line.contains("\"")) {
                        String temp = line.split("\"")[1];
                        String path = Paths.get(ReadFile.getBaseDir(), temp).toString();
                        File file = new File(path);
                        if (file.isFile()) {
                            moduleSet.add(line.split("\"")[1]);
                        }
                    }
                }
                if (line.contains("class")) {
                    hasClass = true;
                }
                if (line.contains("static_cast")) {
                    hasStaticCast = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private Optional<String> parseClass() {
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            ANTLRInputStream inputStream = new ANTLRInputStream(fileInputStream);
            CPP14Lexer lexer = new CPP14Lexer(inputStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            CPP14Parser parser = new CPP14Parser(tokens);
            ParserRuleContext tree = parser.translationunit();

            ClassVisitor visitor = new ClassVisitor(tokens);
            visitor.visit(tree);
            classSet = visitor.getClassSet();

            return Optional.ofNullable(visitor.getFullText());
        } catch (IOException e) {
            System.out.println("File Input is not correct");
        }
        return Optional.empty();
    }

    private Optional<String> parseStaticCast() {
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            ANTLRInputStream inputStream = new ANTLRInputStream(fileInputStream);
            CPP14Lexer lexer = new CPP14Lexer(inputStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            CPP14Parser parser = new CPP14Parser(tokens);
            ParserRuleContext tree = parser.translationunit();

            StaticCastVisitor visitor = new StaticCastVisitor(tokens, classSet);
            visitor.visit(tree);

            return Optional.ofNullable(visitor.getFullText());
        } catch (IOException e) {
            System.out.println("File Input is not correct");
        }

        return Optional.empty();
    }

    void convert() {
        classSet = new HashSet<>();
        if (checkFile(filePath)) {
            if (hasClass) {
                Optional<String> result = parseClass();
                String json = jsonifyClassSet();
                result.ifPresent(x -> writeCppFile(x, json));
            }

            if (hasStaticCast) {
                Optional<String> result = parseStaticCast();
                result.ifPresent(x -> writeCppFile(x, tag));
            }
        }
    }

    private void writeCppFile(String file, String tag) {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(tag);
            bw.newLine();
            bw.write(file);
            System.out.println(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String jsonifyClassSet() {
        StringBuilder sb = new StringBuilder();
        sb.append("/*");
        JSONArray classesArr = new JSONArray();
        for (CppClass cppClass : classSet) {
            JSONDictionary classDict = new JSONDictionary();

            JSONArray superSet = new JSONArray();
            for (CppClass superClass : cppClass.superSet) {
                superSet.add(superClass.className);
            }
            classDict.put("super", superSet);

            JSONArray virtual = new JSONArray();
            for (CppFunction virtualFunction : cppClass.virtualFunctionSet) {
                JSONDictionary function = new JSONDictionary();
                JSONArray params = new JSONArray();
                for (String param : virtualFunction.functionParameter) {
                    params.add(param);
                }
                function.put(virtualFunction.functionName, params);
                virtual.add(function);
            }
            classDict.put("virtual", virtual);

            classesArr.add(classDict);
        }
        sb.append(classesArr.toString());
        sb.append("*/");
        return sb.toString();
    }

}
