package com.kasselapi.data;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Path("/pmservice")
public class PMService {

	/*
	 * MongoDB connection
	 */
	MongoClient client = new MongoClient(
			new MongoClientURI("mongodb://kasselpi:kpi@ds129442.mlab.com:29442/kpisensors"));
	MongoDatabase db = client.getDatabase("kpisensors");

	/*
	 * Access to collection
	 */
	MongoCollection<Document> pmData = db.getCollection("pmData");
	
	@GET
	@Produces("application/json")
	public Response findData() throws JSONException {

		System.out.println("newapi");
		JSONArray data = new JSONArray();

		pmData.find().forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				JSONObject doc = new JSONObject(document.toJson());
				data.put(doc);
			}
		});

		return Response.ok(data.toString()).build();
	}
	
	@GET
	@Path("/{id}")
	@Produces("application/json")
	public Response findData(@PathParam("id") String id) throws JSONException {

		JSONArray data = new JSONArray();

		pmData.find(new Document("_id", new ObjectId(id))).forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				JSONObject doc = new JSONObject(document.toJson());
				data.put(doc);
			}
		});

		return Response.ok(data.toString()).build();
	}
	
	@DELETE
	@Path("/{id}")
	@Produces("application/json")
	public Response deleteData(@PathParam("id") String id) {

		pmData.deleteOne(new Document("_id", new ObjectId(id)));
		return Response.status(200).build();

	}
	
	
}
