package processing.util;

import processing.ReadFile;
import weakclass.CppClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class Checker {
    private Set<CppClass> classSet;
    private String filePath;
    private boolean hasClass;
    private boolean hasStaticCast;
    private HashSet<String> moduleSet;

    public Checker(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Add class's information to classSet
     * @param module Module to get class information
     */
    private void addClassInfo(String module) {
        String classInfoFile = IO.getClassInfoFilePath(module);
        try (BufferedReader br = new BufferedReader(new FileReader(classInfoFile))) {
//            classSet.addAll(ProcessJson.readJson(br.readLine()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if the file contains a keyword.
     */
    public void checkFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            moduleSet = new HashSet<>();
            while ((line = br.readLine()) != null) {
                checkInclude(line);
                checkClass(line);
                checkStaticCast(line); // 3 step
            } // pre-header 를 썼을 때 성능 개선?
        } catch (IOException e) {
            e.printStackTrace();
        }

        checkModule();
    }

    /**
     * If "#include" is included in line, add that path to module set
     * @param line Line in .cc file
     */
    private void checkInclude(String line) {
        if (line.contains("#include")) {
            if (line.contains("\"")) {
                String temp = line.split("\"")[1];
                String path = IO.getFullPath(temp);
                File file = new File(path);
                if (file.isFile()) {
                    moduleSet.add(line.split("\"")[1]);
                }
            }
        }
    }

    /**
     * Check "class" is included in line
     * @param line Line in .cc file
     */
    private void checkClass(String line) {
        if (line.contains("class")) {
            hasClass = true;
        }
    }

    /**
     * Check "static_cast" is included in line
     * @param line Line in .cc file
     */
    private void checkStaticCast(String line) {
        if (line.contains("static_cast")) {
            hasStaticCast = true;
        }
    }

    /**
     * Check module in .cc file
     */
    private void checkModule() {
        for (String module : moduleSet) {
            if (new File(IO.getClassInfoFilePath(module)).exists())
                addClassInfo(module);
            else {
                // TODO parseClass require its module's class information
                Parsing.parseClass(IO.getFullPath(module), null);
            }
        }
    }

    public HashSet<String> getModuleSet() {
        return moduleSet;
    }

    public boolean isHasClass() {
        return hasClass;
    }

    public boolean isHasStaticCast() {
        return hasStaticCast;
    }
}
