// import data from csv to mongoDB

import java.io.BufferedReader;
import java.io.FileReader;
import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

public class Purify {
	public static void main(String[] args) {
		MongoClient mongoClient = new MongoClient();
		MongoDatabase db = mongoClient.getDatabase("recommendation");
		// The name of the file to open.
		// The data is from http://jmcauley.ucsd.edu/data/amazon/
		String fileName = "ratings_Musical_Instruments.csv";
		String line = null;
		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			while ((line = bufferedReader.readLine()) != null) {
				String[] values = line.split(",");
				db.getCollection("ratings").insertOne(new Document()
						.append("user", values[0])
						.append("item", values[1])
						.append("rating", Double.parseDouble(values[2])));
			}
			System.out.println("Import Done!");
			bufferedReader.close();
			mongoClient.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
