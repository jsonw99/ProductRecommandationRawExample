// an easy logic for recommendation

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bson.Document;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MapReduceIterable;
import com.mongodb.client.MongoDatabase;


public class Prediction {
	// potential good results for AWLVQ1NSU3LDS
	private static final String USER_ID = "AWLVQ1NSU3LDS";
	private static final String COLLECTION_NAME = "ratings";
	private static final String USER_COLUMN = "user";
	private static final String ITEM_COLUMN = "item";
	private static final String RATING_COLUMN = "rating";

	public static void main(String[] args) {
		// Init
		MongoClient mongoClient = new MongoClient();
		MongoDatabase db = mongoClient.getDatabase("recommendation");
		// Get USER_ID's purchase records
		List<String> previousItems = new ArrayList<>();
		List<Double> previousRatings = new ArrayList<>();
		FindIterable<Document> iterable = db.getCollection(COLLECTION_NAME).find(new Document(USER_COLUMN, USER_ID));
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				previousItems.add(document.getString(ITEM_COLUMN));
				previousRatings.add(document.getDouble(RATING_COLUMN));
			}
		});
		/**
		var map = function() { 
			if (this.item == "0634029363" && this.rating == 2) {
				emit(this.user, 1); 
				} 
			if (this.item == "B000TZTPQ6" && this.rating == 5) {
				emit(this.user, 1); 
				}
			if (this.item == "B001QE994M" && this.rating == 4) {
				emit(this.user, 1); 
				}
			if (this.item == "B001QE997E" && this.rating == 5) {
				emit(this.user, 1); 
				}
			}
			**/

		// Construct mapper function
		StringBuilder sb = new StringBuilder();
		sb.append("function() {");
		for (int i = 0; i < previousItems.size(); i++) {
			String item = previousItems.get(i);
			Double rating = previousRatings.get(i);
			sb.append("if (this.item == \"");
			sb.append(item);
			sb.append("\" && this.rating == ");
			sb.append(rating);
			sb.append(" ){ emit(this.user, 1); }");
		}
		sb.append("}");
		String map = sb.toString();
		// Construct a reducer function
		String reduce = "function(key, values) {return Array.sum(values)} ";
		// MapReduce
		MapReduceIterable<Document> results = db.getCollection(COLLECTION_NAME).mapReduce(map, reduce);// Need a sorting here
		List<User> similarUsers = new ArrayList<>();
		results.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				String id = document.getString("_id");
				Double value = document.getDouble("value");
				if (!id.equals(USER_ID)) {
					similarUsers.add(new User(id, value));
				}
			}
		});

		Collections.sort(similarUsers);  // sort all other users by similarity
		printList(similarUsers);
		// Get similar users' previous records order by similarity
		Set<String> products = new HashSet<>();
		for (User user : similarUsers) {
			String id = user.getId();
			iterable = db.getCollection(COLLECTION_NAME).find(new Document(USER_COLUMN, id));
			iterable.forEach(new Block<Document>() {
				int currSize = 0; // the number of recommendations from the currently scanned user
				@Override
				public void apply(final Document document) {
					if (currSize < 3 && document.getDouble(RATING_COLUMN) == 5) { // only recommend those 5 star rating, each user recommend no more than 3 items
						String item = document.getString(ITEM_COLUMN);
						if (!previousItems.contains(item)) {
							products.add(document.getString(ITEM_COLUMN));
							currSize++;
						}
					}
				}
			});
			if (products.size() >= 5) { // control the total number of recommendations
				break;
			}
		}
		for (String product : products) {
			System.out.println("Recommended product: " + product);
		}
		mongoClient.close();
	}

	private static void printList(List<User> similarUsers) {
		for (User user : similarUsers) {
			System.out.println(user.getId() + "," + user.getValue());
		}
	}
}