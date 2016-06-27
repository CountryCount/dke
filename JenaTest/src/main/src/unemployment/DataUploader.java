package unemployment;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileManager;
import main.Main;

public class DataUploader
{
    public static void uploadRdf()
    {
        String serviceUri = "http://localhost:3030/unemployed";

        System.out.println("Updating the data of the Fuseki server: " + serviceUri);

        try
        {
            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(serviceUri);
            String path = Main.class.getResource("/resources/population.rdf").getFile();
            FileManager.get().addLocatorClassLoader(Main.class.getClassLoader());
            Model model = FileManager.get().loadModel(path);

            accessor.putModel(model);
            selectRdf();
        }
        catch(Exception ex)
        {
            System.err.println("Could not connect to the Fuseki server to update the data: " + serviceUri + "\n\t*"+ex.getMessage());
        }
    }

    public static void selectRdf()
    {
        String serviceUri = "http://localhost:3030/unemployed";
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
