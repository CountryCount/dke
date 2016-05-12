package main.java;

import org.apache.jena.graph.Graph;
import org.apache.jena.propertytable.graph.GraphCSV;
import org.apache.jena.propertytable.lang.CSV2RDF;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;

public class Main
{
    public static void main(String[] args)
    {
        csvTest();
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
