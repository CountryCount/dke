package main;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Configuration
{
    public static String DATASOURCE_FOLDER = "data";
    public static String POPULATION_FILE = "population.csv";
    public static String PREFIX_UNEMPLOYED_FILES = "arbeitslose_";
    public static String SUFFIX_UNEMPLOYED_FILES = ".csv";
    public static String GENERATED_RDF_FILE = "population.rdf";
    public static String FUSEKI_SERVICE_URL = "http://localhost:3030/unemployed";

    public static int UNEMPLOYED_FROM_YEAR = 2004;
    public static int UNEMPLOYED_TO_YEAR = 2014;

    private static String CONFIG_FILE_PATH;

    static {
        try
        {
            GENERATED_RDF_FILE = File.createTempFile("unemployment_",".rdf").getAbsolutePath();
            CONFIG_FILE_PATH = "config.cfg";
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void init() throws IOException
    {
        System.out.println("Configuring ...");
        int lineNr = 0;
        List<String> lines = null;

        try
        {
            lines = Files.readAllLines(Paths.get(CONFIG_FILE_PATH));
        } catch (NoSuchFileException ex)
        {
            System.out.println("No Configuration file found!\n\t*For configuration a file named config.cfg must be inside the same folder as the executing jar");
            return;
        }

        for(String line : lines)
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

    public static String buildPopulationYearFilePath(int year)
    {
        return Paths.get(DATASOURCE_FOLDER, PREFIX_UNEMPLOYED_FILES + String.valueOf(year) + SUFFIX_UNEMPLOYED_FILES).toAbsolutePath().toString();
    }

    public static String buildPopulationFilePath()
    {
        return Paths.get(DATASOURCE_FOLDER, POPULATION_FILE).toAbsolutePath().toString();
    }
}
