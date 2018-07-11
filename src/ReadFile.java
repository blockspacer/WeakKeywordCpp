import java.io.*;
import java.util.Optional;

public class ReadFile {
    private static int count = 1;

    public static void readCcFile(String filePath) throws IOException {
        try {
            System.out.println(filePath +" : " + count);
            Optional<String> text = Converter.parse(filePath);
//            text.ifPresent(System.out::println);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("No input path.");
        } catch (NullPointerException e) {
            System.out.println(filePath);
        }
        // TODO : Out of memory problem !
        ++count;
    }

    public static void checkDirectory(String dirPath) throws IOException {
        File dir = new File(dirPath);
        if (!dir.isDirectory()) {
            System.out.println("No Directory: " + dir.getPath());
            return ;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file: files) {
                if (file.isDirectory()) {
                    checkDirectory(file.getPath());
                } else if (file.getPath().endsWith(".cc")
                        || file.getPath().endsWith(".cpp")
                        || file.getPath().endsWith(".c++")
                        || file.getPath().endsWith(".h")) {
                    readCcFile(file.getPath());
                }
            }
        }
    }
}