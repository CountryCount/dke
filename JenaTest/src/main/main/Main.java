package main;

import org.apache.jena.graph.Graph;
import org.apache.jena.propertytable.graph.GraphCSV;
import org.apache.jena.propertytable.lang.CSV2RDF;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import unemployment.Commune;
import unemployment.Converter;
import unemployment.DataUploader;
import unemployment.PopulationYear;

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
        Configuration.init();
        Converter.convertCSV();
        DataUploader.uploadRdf();
        selectRdf();
        //convertCSV();
        //testRdf();
        //uploadRdf();
        //selectRdf();
    }

    public static void selectRdf()
    {
        String serviceUri = "http://localhost:3030/unemployed";
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(serviceUri);
        Model model = accessor.getModel();

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
            int i = 0;

            while(rs.hasNext())
            {
                i++;
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

            System.out.println("Gesamt: " + i);
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
