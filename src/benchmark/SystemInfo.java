package benchmark;

/**
 * Created by lundmikkel on 02/01/15.
 */
public class SystemInfo {
    public static void SystemInfo() {
        System.out.printf(
                "# OS:   %s; %s; %s%n",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch")
        );

        System.out.printf(
                "# JVM:  %s; %s%n",
                System.getProperty("java.vendor"),
                System.getProperty("java.version")
        );

        // Hardcoded values for Mikkel's machine
        System.out.println("# CPU:  2,3 GHz Intel Core i7");
        System.out.println("# RAM:  16 GB 1600 MHz DDR3");

        System.out.printf(
                "# Date: %s%n",
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new java.util.Date())
        );
    }
}