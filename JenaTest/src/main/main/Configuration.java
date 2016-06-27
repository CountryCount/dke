package main;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Configuration
{
    public static String DATASOURCE_FOLDER = "data";
    public static String RELATIVE_RESOURCE_PATH = "/resources/";
    public static String PREFIX_UNEMPLOYED_FILES = "arbeitslose_";
    public static String SUFFIX_UNEMPLOYED_FILES = ".csv";
    public static String GENERATED_RDF_FILE = "population.rdf";
    public static String FUSEKI_SERVICE_URL = "http://localhost:3030";
    public static String FUSEKI_DATA_PATH = "/gemeinden";

    private static final String CONFIG_FILE_PATH = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath().substring(1) + "config.cfg";

    public static void init() throws IOException
    {
        System.out.println("Configuring ...");
        int lineNr = 0;
        for(String line : Files.readAllLines(Paths.get(CONFIG_FILE_PATH)))
        {
            lineNr++;
            try
            {
                String[] parts = line.trim().toLowerCase().replace(" ", "").split("=");

                if (parts.length != 2)
                    throw new Exception("Missing value for the config parameter, each config parameter has the following schema: property=value");

                List<Field> fields = Arrays.stream(Configuration.class.getDeclaredFields())
                    .filter(field -> field.getName().toLowerCase().equals(parts[0])).collect(Collectors.toList());

                for(Field f : fields)
                    f.set(null, parts[1]);

                System.out.println("Setting " + parts[0] + " to " + parts[1]);
            }
            catch(Exception ex)
            {
                System.err.println("Could not load line " + lineNr + ": " + line + "\n\t*"+ex.getMessage());
            }
        }
    }
}
