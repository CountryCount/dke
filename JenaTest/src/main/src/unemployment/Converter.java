package unemployment;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import main.Main;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Converter
{
    public static List<Commune> readPopulationYears(int year, Map<String, Commune> communes) throws IOException
    {
        final String CSV_DELIMINATOR = ";";
        final int COL_LAU2CODE_AND_LAU2NAME = 0;
        final int COL_UNEMPLOYED_W = 1;
        final int COL_UNEMPLOYED_M = 4;
        final String csvPath = Main.class.getResource("/resources/arbeitslose_"+year+".csv").getFile().substring(1);
        int lineNr = 0;

        for(String line : Files.readAllLines(Paths.get(csvPath)))
        {
            lineNr++;
            try
            {
                if (!(line.charAt(0) >= '0' && line.charAt(0) <= '9'))
                    continue;
                String[] cols = line.split(CSV_DELIMINATOR);
                String lau2code = cols[COL_LAU2CODE_AND_LAU2NAME].split("-")[0];
                String lau2name = cols[COL_LAU2CODE_AND_LAU2NAME].split("-")[1];
                int unemployedW = Integer.parseInt(cols[COL_UNEMPLOYED_W].replace(".",""));
                int unemployedM = Integer.parseInt(cols[COL_UNEMPLOYED_M].replace(".",""));

                Commune c = communes.get(lau2code);
                if(c == null) continue;
                c.getYears().stream().filter(py -> py.getYear() == year).forEach(py ->
                {
                    py.setWUnemployed(unemployedW);
                    py.setMUnemployed(unemployedM);
                });
            }
            catch(Exception ex)
            {
                System.out.println("Failed to parse line " + lineNr + ": " + ex.getMessage());
            }
        }

        return communes.values().stream().collect(Collectors.toList());
    }

    public static Map<String, Commune> readCommunes() throws IOException
    {
        final String CSV_DELIMINATOR = ",";
        final int COL_NUTS2 = 0;
        final int COL_LAU2CODE = 1;
        final int COL_LAU2NAME = 2;
        final int COL_YEAR = 3;
        final int COL_POPTOTAL = 4;
        final String csvPath = Main.class.getResource("/resources/population.csv").getFile().substring(1);

        final Map<String, Commune> communes = new HashMap<>();
        int lineNr = -1;
        for(String line : Files.readAllLines(Paths.get(csvPath)))
        {
            lineNr++;
            if(lineNr == 0) continue;

            try
            {
                String[] cols = line.split(CSV_DELIMINATOR);
                Commune commune;
                if(communes.containsKey(cols[COL_LAU2CODE]))
                    commune = communes.get(cols[COL_LAU2CODE]);
                else
                {
                    commune = new Commune(cols[COL_LAU2NAME], cols[COL_LAU2CODE], cols[COL_NUTS2]);
                    communes.putIfAbsent(cols[COL_LAU2CODE], commune);
                }
                commune.getYears().add(new PopulationYear(commune, Integer.parseInt(cols[COL_YEAR]), Integer.parseInt(cols[COL_POPTOTAL])));
            }
            catch(Exception ex)
            {
                System.out.println("Failed to parse line " + (lineNr+1) + " : " + ex.getMessage());
            }
        }

        return communes;
    }

    public static void convertCSV() throws IOException
    {
        final String GEMEINDEN_URI = "http://jku.at/gemeinden";
        final String GEMEINDE_URI = "http://jku.at/gemeinden/gemeinde";
        final String LAU2_CODE_URI = "http://jku.at/gemeinden/lau2code";
        final String NUTS2_CODE_URI = "http://jku.at/gemeinden/nuts2code";
        final String LAU2_NAME_URI = "http://jku.at/gemeinden/lau2name";
        final String HAS_YEAR = "http://jku.at/gemeinden/hasYear";
        final String YEAR = "http://jku.at/gemeinden/year";
        final String POP_TOTAL = "http://jku.at/gemeinden/population";
        final String M_UNEMPLOYED = "http://jku.at/gemeinden/mUnemployed";
        final String W_UNEMPLOYED = "http://jku.at/gemeinden/wUnemployed";

        final Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("gemeinden",GEMEINDEN_URI+"/");
        Map<String,Commune> communeMap = readCommunes();

        for(int i = 2004; i <= 2014; i++)
            readPopulationYears(i, communeMap);

        Resource root = model.createResource(GEMEINDEN_URI);
        for (Commune c : communeMap.values())
        {
            Resource communeResource = model.createResource()
                    .addProperty(model.createProperty(LAU2_CODE_URI), c.getLau2_Code())
                    .addProperty(model.createProperty(LAU2_NAME_URI), c.getLau2_Name())
                    .addProperty(model.createProperty(NUTS2_CODE_URI), c.getNuts2());

            for(PopulationYear year : c.getYears())
            {
                communeResource.addProperty(model.createProperty(HAS_YEAR),
                        model.createResource()
                                .addLiteral(model.createProperty(YEAR), year.getYear())
                                .addLiteral(model.createProperty(POP_TOTAL), year.getPopulation())
                                .addLiteral(model.createProperty(M_UNEMPLOYED), year.getMUnemployed() == -1 ? "NDEF" : year.getMUnemployed())
                                .addLiteral(model.createProperty(W_UNEMPLOYED), year.getWUnemployed() == -1 ? "NDEF" : year.getWUnemployed())
                );
            }

            root.addProperty(model.createProperty(GEMEINDE_URI), communeResource);
        }

        String path = Main.class.getResource("/resources/population.rdf").getFile();
        FileOutputStream fs = new FileOutputStream(path);
        model.write(fs);
        fs.flush();
        fs.close();
    }
}
