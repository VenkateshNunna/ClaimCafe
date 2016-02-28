package example.nosql;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.cloudant.client.api.Database;

import example.nosql.CloudantClientMgr;

import com.google.gson.internal.LinkedTreeMap;

@Path("/files")
public class ResourceServlet {
	
	JsonObject allPosts_Json = new JsonObject();
	
 	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
 	public Response uploadFile(
 			@FormDataParam("file") InputStream uploadedInputStream,
 			@FormDataParam("file") FormDataContentDisposition fileDetail,
 			@FormDataParam("file") FormDataBodyPart part,
 			@FormDataParam("message") String msg) {
 		System.out.println("Inside uploadFile")	;
 		Database db = null;
		try
		{
			db = getDB("posts_db");
		}
		catch(Exception re)
		{
			String output = "File failed to upload : ";
			return Response.status(500).entity(output).build();
		}
		System.out.println("Venki:before calling find");
		String id="1234";
		String id_temp = "1234";
		List<HashMap> allDocs = null;
		try {
			allDocs = db.getAllDocsRequestBuilder().includeDocs(true).build().getResponse()
					.getDocsAs(HashMap.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boolean recordExistsWithInputId = false;
		Integer numberOfPosts = 0;
		for(HashMap doc : allDocs){
			String doc_id = (String) doc.get("_id");
			System.out.println("doc_id is "+doc_id);
			if(doc_id.startsWith(id_temp)){
				recordExistsWithInputId = true;
				if(numberOfPosts < Integer.parseInt(doc_id.split(":")[1])){
					numberOfPosts = Integer.parseInt(doc_id.split(":")[1]);
				}
			}
		}
		System.out.println("Venki: recordExistsWithInputid is "+recordExistsWithInputId);
		System.out.println("Venki: number of posts for given id is "+numberOfPosts);
		
		if(recordExistsWithInputId){
			id = id_temp + ":"+ (++numberOfPosts);
		}
		else{
			id = id_temp + ":" + 1;
		}
		System.out.println("id is "+id);
		System.out.println("Venki: Inside null check");
		HashMap<String,Object> newRec = new HashMap<String,Object>();
		newRec.put("_id", id);
		newRec.put("Message", msg);
		newRec.put("Owner", "Venkatesh");
		newRec.put("visibility", "public");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		newRec.put("created", dateFormat.format(date));
		newRec.put("_id", id);
		System.out.println("Venki: Before saving new Rec");
		db.save(newRec);
		System.out.println("Venki: After saving new Rec");
		HashMap<String,Object> doc = db.find(HashMap.class,id);
		
		if(fileDetail != null){
			db.saveAttachment(uploadedInputStream, fileDetail.getFileName(), part.getMediaType().toString(), id, (String)doc.get("_rev"));
		}
			
		System.out.println("file name is "+fileDetail.getFileName());
		System.out.println("fileDetail.Name is "+fileDetail.getName());
		System.out.println("content type is "+fileDetail.getType());
		System.out.println("Media type is "+part.getMediaType());
		
		String output = "File successfully uploaded to : " ;
		System.out.println("Venki:id is "+id);
//		System.out.println("Venki:List of posts are "+getPosts());
 		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("id", doc.get("_id") + "");
		return Response.ok(allPosts_Json.toString()).build();
 	 
	}
 	
 	@POST
	@Path("/addComment")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
 	public Response addComment(
 			@FormDataParam("file") InputStream uploadedInputStream,
 			@FormDataParam("file") FormDataContentDisposition fileDetail,
 			@FormDataParam("file") FormDataBodyPart part,
 			@FormDataParam("comment") String comment,
 			@FormDataParam("conversationNr") String conversationNr) {
 		System.out.println("venki: Inside addComment method");
 		Database db = null;
		try
		{
			db = getDB("comments_db"); 
		}
		catch(Exception re)
		{
			String output = "File failed to upload : ";
			return Response.status(500).entity(output).build();
		}
		
		String searchId = "1234:"+conversationNr;
		System.out.println("venki: searchid is "+searchId);
		List<HashMap> allComments = null;
		try {
			allComments = db.getAllDocsRequestBuilder().includeDocs(true).build().getResponse()
					.getDocsAs(HashMap.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boolean commentsExist = false;
		String newCommentId ;
		Integer lastCommentSeqNr = 0; 
		for(HashMap eachComment : allComments){
			String commentId = (String)eachComment.get("_id");
			System.out.println("venki:each exising comment id is "+commentId);
			if(commentId.startsWith(searchId)){
				commentsExist = true;
				if(lastCommentSeqNr < Integer.parseInt(commentId.split(":")[2])){
					lastCommentSeqNr = Integer.parseInt(commentId.split(":")[2]);
				}
				
			}
		}
		System.out.println("venki: lastCommentSeqNr is "+lastCommentSeqNr);
		newCommentId = searchId +":"+ (++lastCommentSeqNr);
		System.out.println("venki:new comment id is "+newCommentId);
		
		HashMap<String,Object> commentRec = new HashMap<String,Object>();
		commentRec.put("_id", newCommentId);
		commentRec.put("message", comment);
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		commentRec.put("created", dateFormat.format(date));
		db.save(commentRec);
		
		HashMap<String,Object> doc = db.find(HashMap.class,newCommentId);
		if(fileDetail != null){
			db.saveAttachment(uploadedInputStream, fileDetail.getFileName(), part.getMediaType().toString(), newCommentId, (String)doc.get("_rev"));
		}

		
 		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("id", commentRec.get("_id") + "");
		return Response.ok(jsonObject.toString()).build();
 	 
	}
 	
 	@GET
	@Path("/getPosts")
	@Produces(MediaType.APPLICATION_JSON)
 	public Response getPosts() {
 		String id = "1234";
 		Database db = null;
 		String URLTemplate_comment = null;
 		String URLTemplate_post = null; 
		try
		{
			db = getDB("posts_db");
			URLTemplate_post = CloudantClientMgr.getDatabaseURL();
		}
		catch(Exception re)
		{
			String output = "File failed to upload : ";
//			return Response.status(500).entity(output).build();
		}
 		
 		List<HashMap> allPosts = null;
		try {
			allPosts = db.getAllDocsRequestBuilder().includeDocs(true).build().getResponse()
					.getDocsAs(HashMap.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try
		{
			db = getDB("comments_db");
			URLTemplate_comment = CloudantClientMgr.getDatabaseURL();
		}
		catch(Exception re)
		{
			String output = "File failed to upload : ";
//			return Response.status(500).entity(output).build();
		}
 		
 		List<HashMap> allComments = null;
		try {
			allComments = db.getAllDocsRequestBuilder().includeDocs(true).build().getResponse()
					.getDocsAs(HashMap.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 		JsonObject post_json = null;
 		JsonObject comment_json = null;
 		JsonArray comments_jsonArray = null;
 		LinkedTreeMap<String,Object> attachment = null;
 		JsonArray Posts_jsonarray = new JsonArray();
		for(HashMap post : allPosts){
			String postId = (String)post.get("_id");
			
			if(postId.startsWith(id)){
				post_json = new JsonObject();
				comments_jsonArray = new JsonArray();
				post_json.addProperty("id", postId);
				post_json.addProperty("message", (String)post.get("Message"));
				post_json.addProperty("owner", (String)post.get("Owner"));
				
				attachment = (LinkedTreeMap<String, Object>) post.get("_attachments");
				
				if(attachment!=null){
					for (Object key : attachment.keySet()) {
						LinkedTreeMap<String, Object> attach = (LinkedTreeMap<String, Object>) attachment.get(key);
	
						JsonObject attachedObject = new JsonObject();
						// set the content type of the attachment
						attachedObject.addProperty("content_type", attach.get("content_type").toString());
						// append the document id and attachment key to the URL
						attachedObject.addProperty("url", URLTemplate_post + postId + "/" + key);
						// set the key of the attachment
						attachedObject.addProperty("key", key + "");
	
						// add the attachment object to the array
						post_json.add("attachment", attachedObject);
					}
				}
				if(allComments != null && allComments.size() > 0){
					for(HashMap comment : allComments)
					{
						String commentId = (String)comment.get("_id");
						if(commentId.startsWith(postId)){
							comment_json = new JsonObject();
							comment_json.addProperty("id", (String)commentId);
							comment_json.addProperty("message", (String)comment.get("message"));
							
							attachment = (LinkedTreeMap<String, Object>) comment.get("_attachments");
							
							if(attachment!=null){
								for (Object key : attachment.keySet()) {
									LinkedTreeMap<String, Object> attach = (LinkedTreeMap<String, Object>) attachment.get(key);
				
									JsonObject attachedObject = new JsonObject();
									// set the content type of the attachment
									attachedObject.addProperty("content_type", attach.get("content_type").toString());
									// append the document id and attachment key to the URL
									attachedObject.addProperty("url", URLTemplate_comment + commentId + "/" + key);
									// set the key of the attachment
									attachedObject.addProperty("key", key + "");
				
									// add the attachment object to the array
									comment_json.add("attachment", attachedObject);
								}
							}
							
							comments_jsonArray.add(comment_json);
						}
					}
					post_json.add("comments", comments_jsonArray);
				}
				Posts_jsonarray.add(post_json);
			}
		}
 		allPosts_Json.add("Posts", Posts_jsonarray);
// 		JsonObject jsonObject = new JsonObject();
//		jsonObject.addProperty("id", commentRec.get("_id") + "");
		return Response.ok(allPosts_Json.toString()).build();
 	}
 	
	private Database getDB(String dbName)
	{
		return CloudantClientMgr.getDB(dbName);
	}
  }