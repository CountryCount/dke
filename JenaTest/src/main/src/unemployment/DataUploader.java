package unemployment;

import main.Configuration;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileManager;

public class DataUploader
{
    public static void uploadRdf()
    {
        String serviceUri = Configuration.FUSEKI_SERVICE_URL;

        System.out.println("Updating the data of the Fuseki server: " + serviceUri);
        try
        {
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(serviceUri);
            Model model = FileManager.get().loadModel(Configuration.GENERATED_RDF_FILE);

            accessor.putModel(model);
            selectRdf();
        }
        catch(Exception ex)
        {
            System.err.println("Could not connect to the Fuseki server to update the data: " + serviceUri + "\n\t*"+ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void selectRdf()
    {
        String serviceUri = Configuration.FUSEKI_SERVICE_URL;
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(serviceUri);
        Model model = accessor.getModel();

        String sumQueryStr = "" +
                "PREFIX gemeinden: <http://jku.at/gemeinden/> " +
                "SELECT ?name ?year" +
                " WHERE {" +
                "  ?gemeinde gemeinden:lau2name ?name ;" +
                "            gemeinden:hasYear  ?year ." +
                " }";

        Query query = QueryFactory.create(sumQueryStr);
        QueryExecution exec = QueryExecutionFactory.create(query, model);
        try
        {
            ResultSet rs = exec.execSelect();
            int i = 0;
            while(rs.hasNext())
            {
                i++;
                QuerySolution result = rs.nextSolution();
            }

            System.out.println("Total: " + i + " entries have been updated");
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
