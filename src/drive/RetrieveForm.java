package drive;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Children;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class RetrieveForm {
	
	private static Scanner sc;

	private static String CLIENT_ID = "50686604846.apps.googleusercontent.com";
	private static String CLIENT_SECRET = "NgrSAkRfGx91sQ-nfXd53wj4";

	private static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
	
	public static void main(String[] args) {
		File f = start();
		System.out.println("---"+f.getTitle()+"---");
	}
	
	public static File start() {
		sc = new Scanner(System.in);
		Drive service = establishConnection();
		File rightFile = null;
		boolean done = false;
		while(!done) {
			System.out.println("Write parts of or the full name of the file..");
			String fileName = "";
			try {
				fileName = sc.nextLine();
				if(fileName.length() == 0) {
					System.err.println("No name was given..");
					continue;
				}
			} catch(IllegalArgumentException iae) {
				System.err.println("Invalid input.");
				continue;
			}
			List<File> potentialFiles = searchForFile(service, fileName);
			if(potentialFiles.size() == 0) {
				System.out.println("No file by that input exists..");
				continue;
			}
			else if(potentialFiles.size() == 1) {
				rightFile = potentialFiles.get(0);
				break;
			}
			else {
				System.out.println("Multiple files returned..");
				rightFile = pickFile(potentialFiles);
				break;
			}
		}
		return rightFile;
	}

	private static Drive establishConnection() {
		HttpTransport httpTransport = new NetHttpTransport();
		JsonFactory jsonFactory = new JacksonFactory();

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, jsonFactory, CLIENT_ID, CLIENT_SECRET, Arrays.asList(DriveScopes.DRIVE))
		.setAccessType("online")
		.setApprovalPrompt("auto").build();

		String url = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
		System.out.println("Please open the following URL in your browser then type the authorization code:");
		System.out.println("  " + url);
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		GoogleTokenResponse response = null;
		try {
			String code = br.readLine();
			response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
		} catch (IOException ioe) {
			//System.out.println("An error occurred: " + ioe);
			ioe.printStackTrace();
		}		
		
		GoogleCredential credential = new GoogleCredential().setFromTokenResponse(response);

		//Create a new authorized API client
		Drive service = new Drive.Builder(httpTransport, jsonFactory, credential).build();
		
		return service;
		/*//Insert a file  
		File body = new File();
		body.setTitle("My document");
		body.setDescription("A test document");
		body.setMimeType("text/plain");

		java.io.File fileContent = new java.io.File("document.txt");
		FileContent mediaContent = new FileContent("text/plain", fileContent);

		File file = service.files().insert(body, mediaContent).execute();
		System.out.println("File ID: " + file.getId());*/
		
		
		
	}
	
	private static List<File> searchForFile(Drive service, String fileName) {
		List<File> result = new ArrayList<File>();
		FileList request = null;
		try {
			request = service.files().list().setQ("title contains '"+fileName+"'").execute();
			result.addAll(request.getItems());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		do {
//			try {
//				FileList files =  request.execute();
//				
//				result.addAll(files.getItems());
//				request.setPageToken(files.getNextPageToken());
//			} catch(IOException ioe) {
//				System.out.println("An error occurred: " + ioe);
//		        request.setPageToken(null);
//			}
//		} while (request.getPageToken() != null &&
//	             request.getPageToken().length() > 0);
		
		return result;
	}
	
	private static File pickFile(List<File> files) {		
		//int fileNum = -1;
		File file = null;
		boolean done = false;
		
		while(!done) {
			System.out.println("To select a file press the number associated with it..");
			for(int i = 0; i < files.size(); i++) {
				System.out.println("  ("+i+")  "+files.get(i).getTitle());			
			}
			try {
				System.out.print("Press a number: ");
				String in = sc.nextLine();
				//fileNum = sc.nextInt();
				int fileNum = Integer.parseInt(in);
				file = files.get(fileNum);
				//System.out.println();
//				if(fileNum < 0 || fileNum >= files.size()) {
//					System.err.println("That number is not associated with any file.");
//					continue;
//				}
			} catch(IllegalArgumentException iae) {
				System.err.println("Invalid input.");
				continue;
			} catch(InputMismatchException ime) {
				System.err.println("Invalid input.");
				continue;
			} catch(IndexOutOfBoundsException eoobe) {
				System.err.println("Invalid input.");
				continue;
			}
			done = true;
		}
		return file;
	}
	
	/**
	   * Download a file's content.
	   * 
	   * @param service Drive API service instance.
	   * @param file Drive File instance.
	   * @return InputStream containing the file's content if successful,
	   *         {@code null} otherwise.
	   */
	  private static InputStream downloadFile(Drive service, File file) {
	    if (file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
	      try {
	        HttpResponse resp =
	            service.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl()))
	                .execute();
	        return resp.getContent();
	      } catch (IOException e) {
	        // An error occurred.
	        e.printStackTrace();
	        return null;
	      }
	    } else {
	      // The file doesn't have any content stored on Drive.
	      return null;
	    }
	  }
	  
	  /**
	   * Retrieve a list of File resources.
	   *
	   * @param service Drive API service instance.
	   * @return List of File resources.
	   */
	  private static List<File> retrieveAllFiles(Drive service) throws IOException {
	    List<File> result = new ArrayList<File>();
	    Files.List request = service.files().list();

	    do {
	      try {
	        FileList files = request.execute();

	        result.addAll(files.getItems());
	        request.setPageToken(files.getNextPageToken());
	      } catch (IOException e) {
	        System.out.println("An error occurred: " + e);
	        request.setPageToken(null);
	      }
	    } while (request.getPageToken() != null &&
	             request.getPageToken().length() > 0);

	    return result;
	  }
	  
	  /**
	   * Print files belonging to a folder.
	   *
	   * @param service Drive API service instance.
	   * @param folderId ID of the folder to print files from.
	   */
	  private static void printFilesInFolder(Drive service, String folderId)
	      throws IOException {
	    Children.List request = service.children().list(folderId);
	    //List<ChildReference> result = new ArrayList<ChildReference>();

	    do {
	      try {
	        ChildList children = request.execute();	        
	        
	        //result.addAll(children.getItems());

	        for (ChildReference child : children.getItems()) {
	          System.out.println("File Id: " + child.getId());
	        }
	        request.setPageToken(children.getNextPageToken());
	      } catch (IOException e) {
	        System.out.println("An error occurred: " + e);
	        request.setPageToken(null);
	      }
	    } while (request.getPageToken() != null &&
	             request.getPageToken().length() > 0);
	    
	    //return result;
	  }

}
