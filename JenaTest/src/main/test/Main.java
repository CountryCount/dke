package main.test;

import main.java.Commune;
import main.java.PopulationYear;
import org.apache.jena.graph.Graph;
import org.apache.jena.propertytable.graph.GraphCSV;
import org.apache.jena.propertytable.lang.CSV2RDF;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main
{
    public static void main(String[] args) throws IOException
    {
        //convertCSV();
        //testRdf();
        uploadRdf();
    }

    public static void uploadRdf()
    {
        String serviceUri = "http://localhost:3030/gemeinden";
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(serviceUri);
        String path = Main.class.getResource("/main/resources/pop.rdf").getFile();
        FileManager.get().addLocatorClassLoader(Main.class.getClassLoader());
        Model model = FileManager.get().loadModel(path);

        accessor.putModel(model);
        //accessor.add("population",model);
        //accessor.putModel(model);
        //accessor.deleteModel("http://jku.at/gemeinden/");
    }

    public static void testRdf()
    {
        String path = Main.class.getResource("/main/resources/pop.rdf").getFile();
        FileManager.get().addLocatorClassLoader(Main.class.getClassLoader());
        Model model = FileManager.get().loadModel(path);

        String sumQueryStr = "" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "PREFIX gemeinden: <http://jku.at/gemeinden/> " +
            "SELECT ?name ?year ?pop ?mUnEmp ?wUnEmp" +
            " WHERE {" +
            "  ?gemeinde gemeinden:lau2name ?name ;" +
            "            gemeinden:hasYear  ?hasYear ." +
            "  ?hasYear  gemeinden:year     ?year ;" +
            "            gemeinden:population ?pop ;" +
            "            gemeinden:mUnemployed ?mUnEmp ;" +
            "            gemeinden:wUnemployed ?wUnEmp ." +
            //"  FILTER (?mUnEmp > 0 && ?wUnEmp > 0)" +
            " }" +
            " ORDER BY ASC (?name) (?year)";

        Query query = QueryFactory.create(sumQueryStr);
        QueryExecution exec = QueryExecutionFactory.create(query, model);
        try
        {
            ResultSet rs = exec.execSelect();
            String name = "";
            while(rs.hasNext())
            {
                QuerySolution result = rs.nextSolution();
                if(!name.equals(result.getLiteral("name").getString()))
                {
                    name = result.getLiteral("name").getString();
                    System.out.println(name);
                }
                if(result.getLiteral("year").getInt() >= 2004)
                    System.out.println
                        (
                            "\t"
                            + result.getLiteral("year").getString() + " ->"
                            + result.getLiteral("pop").getString() + " => w: "
                            + result.getLiteral("wUnEmp").getString() + " => m: "
                            + result.getLiteral("mUnEmp").getString()
                        );
            }
        }
        catch(Exception ex)
        {
            System.out.println(ex.getMessage());
        }
        finally
        {
            if(exec != null && !exec.isClosed())
                exec.close();
        }
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
                            .addProperty(model.createProperty(YEAR), String.valueOf(year.getYear()))
                            .addProperty(model.createProperty(POP_TOTAL), String.valueOf(year.getPopulation()))
                            .addProperty(model.createProperty(M_UNEMPLOYED), String.valueOf(year.getMUnemployed()))
                            .addProperty(model.createProperty(W_UNEMPLOYED), String.valueOf(year.getWUnemployed()))
                );
            }

            root.addProperty(model.createProperty(GEMEINDE_URI), communeResource);
        }
        FileOutputStream fs = new FileOutputStream("C:\\Users\\Jakob\\Documents\\dke\\JenaTest\\src\\main\\resources\\pop.rdf");
        model.write(fs);
        fs.flush();
        fs.close();
    }

    public static void printCommunes(Map<String, Commune> communes)
    {
        for (Commune c : communes.values())
        {
            System.out.println(c.getLau2_Name());
            for (PopulationYear year : c.getYears())
            {
                System.out.println("\t" + year.getYear() + ": " + year.getPopulation());
            }
        }
    }

    public static List<Commune> readPopulationYears(int year, Map<String, Commune> communes) throws IOException
    {
        final String CSV_DELIMINATOR = ";";
        final int COL_LAU2CODE_AND_LAU2NAME = 0;
        final int COL_UNEMPLOYED_W = 1;
        final int COL_UNEMPLOYED_M = 4;
        final String csvPath = Main.class.getResource("/main/resources/arbeitslose_"+year+".csv").getFile().substring(1);
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
        final String csvPath = Main.class.getResource("/main/resources/population.csv").getFile().substring(1);

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

    public static void modelTest()
    {
        final String GEMEINDEN_URI = "http://jku.at/gemeinden";
        final String GEMEINDE_URI = "http://jku.at/gemeinden/gemeinde";
        final String HAS_CODE_URI = "http://jku.at/gemeinden/code";
        final String HAS_YEAR = "http://jku.at/gemeinden/hasYear";
        final String YEAR = "http://jku.at/gemeinden/year";
        final String POP_TOTAL = "http://jku.at/gemeinden/population";

        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("gemeinden",GEMEINDEN_URI+"/");

        Resource gemeinde1 = model.createResource()
                .addProperty(model.createProperty(HAS_CODE_URI), "1234")
                .addProperty(model.createProperty(HAS_YEAR),
                        model.createResource()
                                .addProperty(model.createProperty(YEAR), "2014")
                                .addProperty(model.createProperty(POP_TOTAL), "197323")
                )
                .addProperty(model.createProperty(HAS_YEAR),
                        model.createResource()
                                .addProperty(model.createProperty(YEAR), "2015")
                                .addProperty(model.createProperty(POP_TOTAL), "199747")
                );

        Resource gemeinde2 = model.createResource()
                .addProperty(model.createProperty(HAS_CODE_URI), "5678");

        model.createResource(GEMEINDEN_URI)
            .addProperty(model.createProperty(GEMEINDE_URI), gemeinde1)
            .addProperty(model.createProperty(GEMEINDE_URI), gemeinde2);

        model.write(System.out);
    }

    public static void staticFileTest()
    {
        FileManager.get().addLocatorClassLoader(Main.class.getClassLoader());
        Model model = FileManager.get().loadModel(Main.class.getResource("/main/resources/data.rdf").getFile());

        String queryStr = "" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "SELECT ?me (COUNT(?other) AS ?knowscount)" +
                "WHERE { " +
                " ?person foaf:name ?me ." +
                " ?person foaf:knows ?other ." +
                " ?other foaf:name ?othername ." +
                " FILTER regex(?me, \"jakob\", \"i\")" +
                "}" +
                "GROUP BY ?me";

        Query query = QueryFactory.create(queryStr);
        QueryExecution exec = QueryExecutionFactory.create(query, model);
        try
        {
            ResultSet rs = exec.execSelect();
            while(rs.hasNext())
            {
                QuerySolution result = rs.nextSolution();
                System.out.print(result.getLiteral("me"));
                System.out.println(" knows " + result.get("knowscount").asLiteral().getInt());
            }
        }
        finally
        {
            if(exec != null && !exec.isClosed())
                exec.close();
        }
    }

    public static void csvTest()
    {
        CSV2RDF.init();
        final String csvPath = Main.class.getResource("/main/resources/population.csv").getFile();
        //final String csvPath = "file:///C:/Users/Jakob/Documents/DKE/JenaTest/population.csv";
        Graph g = new GraphCSV(csvPath);
        Model csvModel = ModelFactory.createModelForGraph(g);
        /*String queryStr = "" +
                "PREFIX csv: <"+csvPath+"#> " +
                "SELECT ?name ?year ?pop " +
                "WHERE { " +
                " ?stadt csv:LAU2_CODE ?code ." +
                " ?stadt csv:LAU2_NAME ?name ." +
                " ?stadt csv:YEAR ?year ." +
                " ?stadt csv:POP_TOTAL ?pop ." +
                " FILTER (?pop > 0)" +
                "} " +
                "GROUP BY ?name ?year ?pop " +
                "ORDER BY DESC (?pop)";*/
        String queryStrMax = "" +
                "PREFIX csv: <"+csvPath+"#> " +
                "SELECT ?year ?name2 ?pop2 " +
                "WHERE {" +
                " ?stadt csv:YEAR ?year ." +
                " ?stadt csv:LAU2_NAME ?name2 ." +
                " ?stadt csv:POP_TOTAL ?pop2 ." +
                " FILTER(?name2 = ?name && ?pop2 = ?max)" +
                " {" +
                "  SELECT ?name (MAX(?pop) AS ?max) " +
                "  WHERE { " +
                "   ?stadt csv:LAU2_CODE ?code ." +
                "   ?stadt csv:LAU2_NAME ?name ." +
                "   ?stadt csv:POP_TOTAL ?pop ." +
                "  } " +
                "  GROUP BY ?name " +
                "  ORDER BY DESC (?max)" +
                " }" +
                "}";

        String sumQueryStr = "" +
                "PREFIX csv: <"+csvPath+"#> " +
                "SELECT ?year (SUM(?pop) AS ?sumpop) " +
                " WHERE {" +
                "  ?stadt csv:YEAR ?year ;" +
                "  csv:POP_TOTAL ?pop ." +
                " }" +
                " GROUP BY ?year " +
                " ORDER BY DESC (?sumpop)";

        Query query = QueryFactory.create(sumQueryStr);
        QueryExecution exec = QueryExecutionFactory.create(query, csvModel);
        try
        {
            ResultSet rs = exec.execSelect();
            while(rs.hasNext())
            {
                QuerySolution result = rs.nextSolution();
                System.out.println
                (   result.getLiteral("year").getString() + ": "
                  //+ result.getLiteral("year").getString() + " -> "
                  + result.getLiteral("sumpop").getInt()
                );
            }
        }
        catch(Exception ex)
        {
            System.out.println(ex.getMessage());
        }
        finally
        {
            if(exec != null && !exec.isClosed())
                exec.close();
        }
    }
}