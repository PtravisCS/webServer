package multithreaded_serv;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
    
    try (Scanner in = new Scanner(socket.getInputStream()); OutputStream out = socket.getOutputStream()) {

      String req = in.nextLine(); //Recieve GET request, store as req
      System.out.println("Client: " + req);
      String reqFile = req.split(" ")[1].substring(1); //Deterimine requested file name and then strip the slash from the file name
      String[] reqFileExtArr = reqFile.split("\\.");
      String reqFileExt = "";
      if (reqFileExtArr.length > 1) {
        reqFileExt = reqFileExtArr[1];
      }
      System.out.println(reqFileExt);
      File file = new File(reqFile); //Create File Object
      System.out.println(file);
      String hello = "";
      if (file.exists() && !file.isDirectory()) { //File exists and is not a directory

        hello += "HTTP/1.1 200 \r\n";
        Scanner fin = new Scanner(file); //Set up File Reader (AKA scanner)

        switch(reqFileExt) {
          case "html":
            text(fin, hello, out, "html");
            break;
          case "php":
            php(out, hello, file);
            break;
          case "ico":
            ico(out, hello, file);
            break;
          case "png":
            picture(out, hello, file, "png");
            break;
          case "jpg":
            picture(out, hello, file, "jpg");
            break;
          case "css":
            text(fin, hello, out, "css");
            break;
          case "js":
            text(fin, hello, out, "javascript");
            break;
          default:
            httpStatusCode(out, hello, "403");
            break;
        }

      } else if (file.exists() && file.isDirectory()){ //if file is a directory
        httpStatusCode(out, hello, "403");
      } else { //otherwise file probably doesn't exist
        httpStatusCode(out, hello, "404");
      }

      in.close();
      out.close();
      socket.close();

    } catch (IOException e) {
      System.out.println("Server: IOException has Occurred");
      System.out.println(e);
    } catch (Exception e) {
      System.out.println("Server: Exception Happened: ");
      System.out.println(e);
    }
  }
  
  /**
   * Used for sending a text string to client.
   * @param fin
   * @param hello
   * @param out
   * @param textType
   * @throws IOException 
   */
  private void text(Scanner fin, String hello, OutputStream out, String textType) throws IOException {

    hello += "Content-Type: text/" + textType + "\r\n";
    hello += "Connection: Keep-Alive\r\n";
    respond(fin, hello, out);

  }
  
  /**
   * Used for responding to requests for text based files such as HTML, CSS, JS, etc.
   * Will open and read the file and post it to client.
   * @param hello
   * @param out
   * @param file
   * @param textType
   * @throws IOException 
   */
  private void text(String hello, OutputStream out, String file, String textType) throws IOException {
    
    hello += "Content-Type: text/" + textType + "\r\n";
    hello += "Connection: Keep-Alive\r\n";
    hello += file;
    respond(hello, out);
    
  }

  /**
   * Used to send a HTTP status code to client.
   * @param out OutputStream used to respond to client.
   * @param hello String used to represent message to client.
   * @param statusCode String containing the status code to respond with.
   * @throws IOException 
   */
  private void httpStatusCode(OutputStream out, String hello, String statusCode) throws IOException {

    hello += "HTTP/1.1 " + statusCode + " \r\n";
    hello += "Connection: close\r\n";
    hello += "\r\n";
    respond(hello, out);
    
  }
  /**
   * Used to handle requests for PHP scripts.
   * Requires that PHP be installed on the target system and be in the path.
   * Runs requested file using PHP then sends response to client.
   * @param out OutputStream used to respond to client.
   * @param hello String used represent message to client.
   * @param file File to parse using PHP.
   * @throws IOException 
   */
  private void php(OutputStream out, String hello, File file) throws IOException {
    Runtime rt = Runtime.getRuntime();
    String[] commandAndOptions = {"php.exe", "-f", System.getProperty("user.dir") + "\\" + file};
    Process proc = rt.exec(commandAndOptions);

    BufferedReader stdIn = new BufferedReader(new InputStreamReader(proc.getInputStream()));
    BufferedReader stdErr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

    String line;
    StringBuilder output = new StringBuilder();

    while ((line = stdIn.readLine()) != null) {
      output.append(line).append("\r\n");
    }

    StringBuilder err = new StringBuilder();

    while ((line = stdErr.readLine()) != null) {
      err.append(line);
    }

    if (!"".equals(err.toString())) {
      System.out.println("Err: " + err);
    }

    text(hello, out, output.toString(), "html");

  }
  
  /**
   * Used to handle incoming request for an image file.
   * @param out OutputStream used to respond to client.
   * @param hello String used represent message to client.
   * @param file File to send to client.
   * @param imgType String representing the image's Content-Type
   * @throws IOException 
   */
  private void picture(OutputStream out, String hello, File file, String imgType) throws IOException {

    hello += "Content-Type: image/" + imgType + "\r\n";
    hello += "Connection: Keep-Alive\r\n";
    hello += "\r\n";
    respond(hello, file, out);

  }
  
  /**
   * Used to handle incoming requests for an icon file.
   * @param out OutputStream used to respond to client.
   * @param hello String used to represent message to client.
   * @param file File to be sent to client.
   * @throws IOException 
   */
  private void ico(OutputStream out, String hello, File file) throws IOException {
          
    hello += "Content-Type: image/x-icon\r\n";
    hello += "Content-Length: " + 108000 + "\r\n";
    hello += "Transfer-Encoding: identity\r\n";
    hello += "Connection: Keep-Alive\r\n";
    hello += "\r\n";
    respond(hello, file, out);

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

    System.out.println("Server Response: " + hello);
    byte[] b = hello.getBytes(); //Convert hello to bytes and store resultant bytes in byte array.
    out.write(b); 

    fin.close();
    out.close();
  }
  
  /**
   * Used to output a pre-defined string.
   * @param hello String containing the HTTP response header data.
   * @param out OutputStream connected to the client.
   * @throws IOException 
   */
  public static void respond(String hello, OutputStream out) throws IOException {
    System.out.println("Server Response: " + hello);
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

    System.out.println("Server Response: " + hello);
    ByteArrayOutputStream bOut = new ByteArrayOutputStream();
    bOut.write(b);
    bOut.write(i);
    byte[] c = bOut.toByteArray();

    out.write(c);
  }
}