package com.kasselapi.data;

import java.io.StringWriter;
import java.util.Date;
import java.util.Random;
import java.time.Instant;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.*;

@Path("/pmservice")
public class PMService {

	/*
	 * MongoDB connection and accessing DB collection
	 */
	MongoClient client = new MongoClient(
			new MongoClientURI("mongodb://kasselpi:kpi@ds129442.mlab.com:29442/kpisensors"));
	MongoDatabase db = client.getDatabase("kpisensors");
	MongoCollection<Document> pmData = db.getCollection("pmData");
	
	/*
	 * Configure settings to return JSON response with id as string and date format UTC ISO8601.
	 */
	JsonWriterSettings settings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED)
			.objectIdConverter((value, writer) -> writer.writeString(value.toHexString()))
			.dateTimeConverter((value, writer) -> writer.writeString((Instant.ofEpochMilli(value).toString())))
			.build();

	@GET
	@Produces("application/json")
	public Response findData() throws JSONException {

		System.out.println("newapi");
		JSONArray data = new JSONArray();

		pmData.find().forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				JSONObject doc = new JSONObject(document.toJson(settings));
				data.put(doc);
			}
		});

		return Response.ok(data.toString()).build();
	}

	@GET
	@Path("/{id}")
	@Produces("application/json")
	public Response findDataById(@PathParam("id") String id) throws JSONException {

		JSONArray data = new JSONArray();
		
		pmData.find(new Document("_id", new ObjectId(id))).forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				JSONObject doc = new JSONObject(document.toJson(settings));
				data.put(doc);
			}
		});

		return Response.ok(data.toString()).build();
	}

	@GET
	@Path("/query")
	@Produces("application/json")
	public Response findDataByParams(@Context UriInfo info) throws JSONException {
		
		// Error
		double error = Double.parseDouble(info.getQueryParameters().getFirst("error"));
		
		// Error factor
		double factor = Double.parseDouble(info.getQueryParameters().getFirst("factor"));
		
		// Value
		String value = info.getQueryParameters().getFirst("value");	
		
		FindIterable<Document> docs;
		JSONArray data;
		
		if (info.getQueryParameters().containsKey("amount")){
			//optional: max number to return 
			int amount = Integer.parseInt(info.getQueryParameters().getFirst("amount"));
			docs = pmData.find().limit(amount);
		} else if (info.getQueryParameters().containsKey("startDate") && info.getQueryParameters().containsKey("endDate")) {
			//optional: range startDate and endDate in unix 
			long startDate = Long.parseLong(info.getQueryParameters().getFirst("startDate"));
			long endDate = Long.parseLong(info.getQueryParameters().getFirst("endDate"));
			docs = pmData.find( and(gte("date", new Date(startDate)), lt("date", new Date(endDate))));
		} else {
			// requesting all data documents
			docs = pmData.find();
		}
		
		data = modifyWithError(docs, error, factor, value);
		
		return Response.ok(data.toString()).build();
	}
	
	/**
	 * Helper method to modify data and return with errors.
	 * Data with modifications has hasError flag set to true.
	 * 
	 * @param docs		search filters: none, max number of results, or time range
	 * @param error		error rate
	 * @param factor	multiple value by this factor
	 * @param value		value to b modified
	 * @return			JSONArray of modified results 
	 */
	private JSONArray modifyWithError(FindIterable<Document> docs, double error, double factor, String value) {

		JSONArray data = new JSONArray();

		docs.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				JSONObject doc = new JSONObject(document.toJson(settings));

				Random rand = new Random();
				if (rand.nextDouble() < error) {
					doc.put("hasError", true);
					JSONObject valueWithError = doc.getJSONObject(value);
					valueWithError.put("value", valueWithError.getDouble("value") * factor);
				} else {
					doc.put("hasError", false);
				}

				data.put(doc);
			}
		});

		return data;
	}

	@DELETE
	@Path("/{id}")
	@Produces("application/json")
	public Response deleteData(@PathParam("id") String id) {

		pmData.deleteOne(new Document("_id", new ObjectId(id)));
		return Response.status(200).build();

	}

}
