package multithreaded_serv;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Paul Travis
 */
public class MultiThreaded_Serv {

  public static void main(String[] args) {
    Boolean run = true; //Not really different than while(true) but it allows for a mutable value so that the program theoretically could be set up to exit in the future if desired.
    
    try (ServerSocket servSock = new ServerSocket(8080);){ //This feels messy as the socket is thrown away and re-created every time the program loops, but I couldn't find a better way to do it.
      while(run) {
          Socket socket = servSock.accept(); //Wait for connection and accept it
          ProcessReq procReq = new ProcessReq(socket); //Create main server class and feed it the previously created socket.
          Thread t = new Thread(procReq); //Create new thread using the main server class
          t.start(); //start the thread
      }
    } catch (IOException e) {
      System.out.println("IOException Occurred, ");
      System.out.println(e);
    } catch (Exception e) {
      System.out.println("That Was Unexpected");
      System.out.println("Exception Ocurred :" );
      System.out.println(e);
      run = false;
    }
  }

}
