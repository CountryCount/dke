package unemployment;

import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
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
        }
        catch(Exception ex)
        {
            System.err.println("Could not connect to the Fuseki server to update the data: " + serviceUri + "\n\t*"+ex.getMessage());
        }
    }
}
