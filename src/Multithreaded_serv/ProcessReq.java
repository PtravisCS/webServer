package multithreaded_serv;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Scanner;

/**
 *
 * @author Paul Travis
 */
public class ProcessReq implements Runnable {
  Socket socket;

  ProcessReq(Socket socket){
    this.socket = socket;
  }

  @Override
  public void run() {
    try{
      System.out.println("Accepted Client");

      //Create Socket IO Objects
      Scanner in = new Scanner(socket.getInputStream());
      OutputStream out = socket.getOutputStream();

      //Handle GET request.
      String req = in.nextLine(); //Recieve GET request, store as req
      String reqFile = req.split(" ")[1].substring(1); //Deterimine requested file and then strip the slash from the file name
      System.out.println(reqFile); //Print Name of Requested File to Console
      String[] reqFileExtArr = reqFile.split("\\.");
      String reqFileExt = "";
      if (reqFileExtArr.length > 1) {
        reqFileExt = reqFileExtArr[1];
      }
      System.out.println(reqFileExt);
      File file = new File(reqFile); //Encapsulate File
      String hello = "";
      if (file.exists() && !file.isDirectory()) { //File exists and is not a directory

        hello += "HTTP/1.1 200 \r\n";
        if (reqFileExt.equals("html")) { //If File is an HTML file
          Scanner fin = new Scanner(file); //Set up File Reader (AKA scanner)

          hello += "Content-Type: text/html\r\n";
          hello += "Connection: Keep-Alive\r\n";
          respond(fin, hello, out);			
        } else if(reqFileExt.equals("ico")) { //if File is an Icon

          hello += "Content-Type: image/x-icon\r\n";
          hello += "Content-Length: " + 108000 + "\r\n";
          hello += "Transfer-Encoding: identity\r\n";
          hello += "Connection: Keep-Alive\r\n";
          hello += "\r\n";
          respond(hello, file, out);
        }  else if (reqFileExt.equals("png")) { //if File is a png

          hello += "Content-Type: image/png\r\n";
          hello += "Connection: Keep-Alive\r\n";
          hello += "\r\n";
          respond(hello, file, out);
        } else if (reqFileExt.equals("css")) { //if File is a CSS file
          Scanner fin = new Scanner(file);

          hello += "Content-Type: text/css\r\n";
          hello += "Connection: Keep-Alive\r\n";
          hello += "\r\n";
          respond(fin, hello, out);
        } else if (reqFileExt.equals("js")) { //if File is a javaScript script
          Scanner fin = new Scanner(file);

          hello += "Content-Type: text/javascript\r\n";
          hello += "Connection: Keep-Alive\r\n";
          hello += "\r\n";
          respond(fin, hello, out);
        } else { //Not in the supported file set, deny access
          hello = "";
          hello += "HTTP/1.1 403 \r\n";
          hello += "Connection: close\r\n";
          hello += "\r\n";
          respond(hello,out);
        }
      } else if (file.exists() && file.isDirectory()){ //if file is a directory

        System.out.println("Client Requested: " + file.getName()); //log requested directory to console.
        hello += "HTTP/1.1 403 \r\n";
        hello += "Connection: close\r\n";
        hello += "\r\n";
        respond(hello,out);
      } else { //otherwise file probably doesn't exist

        System.out.println("Client Requested: " + file.getName()); //log requested file to console.
        hello += "HTTP/1.1 404 \r\n";
        hello += "Connection: close\r\n";
        hello += "\r\n";
        respond(hello, out);
      }
      in.close();
      out.close();
      socket.close();

    } catch (IOException e) {
      System.out.println("IOException has Occurred");
      System.out.println(e);
    } catch (Exception e) {
      System.out.println("Exception Happened: ");
      System.out.println(e);
    }
  }

  /**
   * Reads in a File, converts it to UTF-8 binary, and sends it along the wire to the receiver.
   * @param fin Scanner linked to the file to send back to the server.
   * @param hello String containing the HTTP response header data.
   * @param out OutputStream connected to the client.
   * @throws IOException 
   */
  public static void respond(Scanner fin, String hello, OutputStream out) throws IOException {

    hello += "\r\n\r\n"; //Make sure there is enough carriage return newlines between the header and the file so the file doesn't get ignored/truncated.
    if (fin.hasNextLine()) { //If there is more file to read read it.
      hello += fin.nextLine(); //Append next line of the file to the string.
    }

    while (fin.hasNextLine()) { //While there is more file to read read it.
      hello += "\r\n" + fin.nextLine(); //Append next line of the file to the string.
    }
    hello += "\r\n"; //Append Carriage Return and Newline to the string.

    System.out.println("Sending: " + hello);
    byte[] b = hello.getBytes(); //Convert hello to bytes and store resultant bytes in byte array.
    out.write(b); 

    fin.close();
    out.close();
  }

  /**
   * Sends one of the HTTP response headers such as 403 or 404 to the specified OutStream connection.
   * @param hello String containing the HTTP response header data.
   * @param out OutputStream connected to the client.
   * @throws IOException 
   */
  public static void respond(String hello, OutputStream out) throws IOException {
    System.out.println("Sending: " + hello);
    byte[] b = hello.getBytes();
    out.write(b);
  }

  /**
   * Reads in an image file as a binary array then sends it over the wire to the receiver.
   * @param hello String containing the HTTP response header data.
   * @param img a File object containing the image to be sent to the server
   * @param out an OutputStream connected to the client.
   * @throws IOException 
   */
  public static void respond(String hello, File img, OutputStream out) throws IOException {
    byte[] b = hello.getBytes(); //Convert the String to it's concordant bytes
    byte[] i = Files.readAllBytes(img.toPath());

    System.out.println("Sending: " + hello);
    ByteArrayOutputStream bOut = new ByteArrayOutputStream();
    bOut.write(b);
    bOut.write(i);
    byte[] c = bOut.toByteArray();

    out.write(c);
  }
}